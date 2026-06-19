package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.domain.UserPreferenceType;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidateFilter;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCondition;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.dto.RecommendationStatus;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import com.ssafy.zipdaum.recent.dto.RecentPropertyScoreFactor;
import com.ssafy.zipdaum.recent.mapper.RecentPropertyMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

  private static final int RECOMMENDATION_SURROUNDING_RADIUS_METERS = 1000;
  private static final int DEFAULT_RECOMMENDATION_SIZE = 20;
  private static final int RECENT_PROPERTY_SCORE_BONUS = 5;
  private static final int MAX_RECENT_PROPERTY_SCORE_BONUS = 15;
  private static final BigDecimal PRICE_PARTIAL_MATCH_MULTIPLIER = BigDecimal.valueOf(1.1);
  private static final BigDecimal AREA_PARTIAL_MATCH_MULTIPLIER = BigDecimal.valueOf(0.9);

  private final RecommendationMapper recommendationMapper;
  private final RecentPropertyMapper recentPropertyMapper;
  private final UserPreferenceService userPreferenceService;
  private final SurroundingService surroundingService;
  private final RecommendationScoreService recommendationScoreService;

  @Override
  @Transactional(readOnly = true)
  public PropertyRecommendationScore findPropertyRecommendationScore(Long userId, Long propertyId) {
    PropertyRecommendationCandidate property =
        recommendationMapper.selectPropertyRecommendationCandidate(propertyId);

    if (property == null) {
      log.warn("주택 맞춤 조건 적합도 조회 실패 - 존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    List<UserPreferenceResponse> preferences = userPreferenceService.findPreferences(userId);
    SurroundingSummaryResponse surroundingSummary = findSurroundingSummary(property, preferences);

    PropertyRecommendationScore score = recommendationScoreService.calculateMatchScore(
        property,
        preferences,
        surroundingSummary
    );

    log.info("주택 맞춤 조건 적합도 계산 완료 score={}, conditionCount={}",
        score.getScore(), score.getConditions().size());
    return score;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PropertyRecommendationResponse> findPropertyRecommendations(Long userId) {
    List<UserPreferenceResponse> preferences = userPreferenceService.findPreferences(userId);
    Map<Long, RecentPropertyScoreFactor> recentPropertyScoreFactors =
        findRecentPropertyScoreFactors(userId);
    if (!hasEvaluablePreference(preferences)) {
      return findRecentPropertyFallbackRecommendations(recentPropertyScoreFactors);
    }

    PropertyRecommendationCandidateFilter filter = toCandidateFilter(preferences);
    List<PropertyRecommendationCandidate> candidates =
        recommendationMapper.selectPropertyRecommendationCandidates(filter);

    List<ScoredProperty> scoredProperties = new ArrayList<>();
    for (PropertyRecommendationCandidate candidate : candidates) {
      SurroundingSummaryResponse surroundingSummary = findSurroundingSummary(candidate, preferences);
      PropertyRecommendationScore score = recommendationScoreService.calculateMatchScore(
          candidate,
          preferences,
          surroundingSummary
      );
      score = applyRecentPropertyScore(
          score,
          recentPropertyScoreFactors.get(candidate.getId())
      );
      if (isPositiveScore(score)) {
        scoredProperties.add(new ScoredProperty(candidate, score, surroundingSummary));
      }
    }

    List<PropertyRecommendationResponse> recommendations = scoredProperties.stream()
        .sorted(recommendationComparator(preferences))
        .limit(DEFAULT_RECOMMENDATION_SIZE)
        .map(scoredProperty -> toResponse(scoredProperty.candidate(), scoredProperty.score()))
        .toList();

    log.info("사용자 맞춤 주택 추천 목록 조회 완료 resultCount={}", recommendations.size());
    return recommendations;
  }

  private List<PropertyRecommendationResponse> findRecentPropertyFallbackRecommendations(
      Map<Long, RecentPropertyScoreFactor> recentPropertyScoreFactors) {
    if (recentPropertyScoreFactors.isEmpty()) {
      log.debug("사용자 맞춤 주택 추천 목록 조회 완료 resultCount=0");
      return List.of();
    }

    List<ScoredProperty> scoredProperties = new ArrayList<>();
    for (RecentPropertyScoreFactor factor : recentPropertyScoreFactors.values()) {
      PropertyRecommendationCandidate candidate =
          recommendationMapper.selectPropertyRecommendationCandidate(factor.getPropertyId());
      if (candidate == null) {
        continue;
      }
      scoredProperties.add(new ScoredProperty(candidate, createRecentPropertyScore(factor), null));
    }

    List<PropertyRecommendationResponse> recommendations = scoredProperties.stream()
        .sorted(recommendationComparator(List.of()))
        .limit(DEFAULT_RECOMMENDATION_SIZE)
        .map(scoredProperty -> toResponse(scoredProperty.candidate(), scoredProperty.score()))
        .toList();

    log.info("사용자 맞춤 주택 추천 목록 조회 완료 resultCount={}", recommendations.size());
    return recommendations;
  }

  private Map<Long, RecentPropertyScoreFactor> findRecentPropertyScoreFactors(Long userId) {
    List<RecentPropertyScoreFactor> factors =
        recentPropertyMapper.selectRecentPropertyScoreFactors(userId);
    if (factors == null || factors.isEmpty()) {
      return Map.of();
    }
    return factors.stream()
        .collect(Collectors.toMap(
            RecentPropertyScoreFactor::getPropertyId,
            factor -> factor,
            (left, right) -> left
        ));
  }

  private PropertyRecommendationScore applyRecentPropertyScore(
      PropertyRecommendationScore score,
      RecentPropertyScoreFactor recentPropertyScoreFactor) {
    if (recentPropertyScoreFactor == null
        || score.getRecommendationStatus() != RecommendationStatus.EVALUATED
        || score.getScore() == null) {
      return score;
    }

    int bonus = calculateRecentPropertyScoreBonus(recentPropertyScoreFactor.getViewCount());
    int adjustedScore = Math.min(100, score.getScore() + bonus);
    List<PropertyRecommendationCondition> conditions = new ArrayList<>(score.getConditions());
    conditions.add(toRecentPropertyCondition(recentPropertyScoreFactor, bonus));
    return new PropertyRecommendationScore(adjustedScore, conditions);
  }

  private PropertyRecommendationScore createRecentPropertyScore(
      RecentPropertyScoreFactor recentPropertyScoreFactor) {
    int bonus = calculateRecentPropertyScoreBonus(recentPropertyScoreFactor.getViewCount());
    return new PropertyRecommendationScore(
        bonus,
        List.of(toRecentPropertyCondition(recentPropertyScoreFactor, bonus))
    );
  }

  private PropertyRecommendationCondition toRecentPropertyCondition(
      RecentPropertyScoreFactor recentPropertyScoreFactor,
      int bonus) {
    return new PropertyRecommendationCondition(
        "RECENT_PROPERTY",
        "최근 본 주택",
        String.valueOf(recentPropertyScoreFactor.getViewCount()),
        null,
        true,
        bonus,
        "최근 확인한 주택"
    );
  }

  private int calculateRecentPropertyScoreBonus(Integer viewCount) {
    int safeViewCount = viewCount == null ? 1 : Math.max(viewCount, 1);
    return Math.min(
        safeViewCount * RECENT_PROPERTY_SCORE_BONUS,
        MAX_RECENT_PROPERTY_SCORE_BONUS
    );
  }

  private PropertyRecommendationCandidateFilter toCandidateFilter(
      List<UserPreferenceResponse> preferences) {
    String region = null;
    Long salePriceMax = null;
    Long depositMax = null;
    Long monthlyRentMax = null;
    BigDecimal minExclusiveArea = null;
    boolean monthlyRentPreferred = hasPreference(preferences, UserPreferenceType.MONTHLY_RENT);

    for (UserPreferenceResponse preference : preferences) {
      UserPreferenceType type = parsePreferenceType(preference.getCode());
      if (type == null) {
        continue;
      }

      switch (type) {
        case SALE_PRICE -> salePriceMax = toPartialPriceMax(preference.getValue());
        case DEPOSIT -> depositMax = toPartialPriceMax(preference.getValue());
        case MONTHLY_RENT -> monthlyRentMax = toPartialPriceMax(preference.getValue());
        case AREA -> minExclusiveArea = toPartialAreaMin(preference.getValue());
        case REGION -> region = normalize(preference.getValue());
        case BUILD_YEAR, BUS, SUBWAY, HOSPITAL, CCTV, PARK -> {
        }
      }
    }

    return new PropertyRecommendationCandidateFilter(
        region,
        salePriceMax,
        depositMax,
        monthlyRentMax,
        minExclusiveArea,
        monthlyRentPreferred
    );
  }

  private Long toPartialPriceMax(String value) {
    BigDecimal price = parseBigDecimal(value);
    if (price == null) {
      return null;
    }
    return price.multiply(PRICE_PARTIAL_MATCH_MULTIPLIER)
        .setScale(0, RoundingMode.DOWN)
        .longValue();
  }

  private BigDecimal toPartialAreaMin(String value) {
    BigDecimal area = parseBigDecimal(value);
    if (area == null) {
      return null;
    }
    return area.multiply(AREA_PARTIAL_MATCH_MULTIPLIER);
  }

  private SurroundingSummaryResponse findSurroundingSummary(
      PropertyRecommendationCandidate property,
      List<UserPreferenceResponse> preferences) {
    if (!hasFacilityPreference(preferences)) {
      return null;
    }
    if (property.getLatitude() == null || property.getLongitude() == null) {
      log.debug("주택 좌표 정보 없음 - 주변시설 조건은 불일치 처리 propertyId={}", property.getId());
      return null;
    }

    return surroundingService.findSurroundingSummary(
        property.getLatitude(),
        property.getLongitude(),
        RECOMMENDATION_SURROUNDING_RADIUS_METERS
    );
  }

  private boolean hasFacilityPreference(List<UserPreferenceResponse> preferences) {
    return preferences.stream()
        .anyMatch(preference ->
            isFacilityCode(preference.getCode()) && "true".equalsIgnoreCase(preference.getValue())
        );
  }

  private boolean hasEvaluablePreference(List<UserPreferenceResponse> preferences) {
    return preferences.stream()
        .anyMatch(this::isEvaluablePreference);
  }

  private boolean isEvaluablePreference(UserPreferenceResponse preference) {
    UserPreferenceType type = parsePreferenceType(preference.getCode());
    if (type == null) {
      return false;
    }
    if (!isFacilityCode(type.name())) {
      return true;
    }
    return "true".equalsIgnoreCase(preference.getValue());
  }

  private boolean isFacilityCode(String code) {
    if (code == null) {
      return false;
    }
    String normalizedCode = code.trim().toUpperCase();
    return "BUS".equals(normalizedCode)
        || "SUBWAY".equals(normalizedCode)
        || "HOSPITAL".equals(normalizedCode)
        || "CCTV".equals(normalizedCode)
        || "PARK".equals(normalizedCode);
  }

  private Comparator<ScoredProperty> recommendationComparator(
      List<UserPreferenceResponse> preferences) {
    List<UserPreferenceResponse> sortedPreferences = preferences.stream()
        .sorted(Comparator.comparing(
            UserPreferenceResponse::getPriority,
            Comparator.nullsLast(Comparator.naturalOrder())
        ))
        .toList();

    return (left, right) -> {
      for (UserPreferenceResponse preference : sortedPreferences) {
        BigDecimal leftValue = calculateSortValue(left, preference, sortedPreferences);
        BigDecimal rightValue = calculateSortValue(right, preference, sortedPreferences);
        int compared = rightValue.compareTo(leftValue);
        if (compared != 0) {
          return compared;
        }
      }

      int scoreCompared = Integer.compare(scoreValue(right.score()), scoreValue(left.score()));
      if (scoreCompared != 0) {
        return scoreCompared;
      }
      return Long.compare(right.candidate().getId(), left.candidate().getId());
    };
  }

  private BigDecimal calculateSortValue(
      ScoredProperty scoredProperty,
      UserPreferenceResponse preference,
      List<UserPreferenceResponse> preferences) {
    PropertyRecommendationCondition condition = findCondition(scoredProperty.score(), preference);
    if (condition == null || !condition.isMatched()) {
      return BigDecimal.valueOf(Long.MIN_VALUE);
    }

    UserPreferenceType type = parsePreferenceType(preference.getCode());
    if (type == null) {
      return BigDecimal.ZERO;
    }

    PropertyRecommendationCandidate property = scoredProperty.candidate();
    return switch (type) {
      case SALE_PRICE -> subtract(parseBigDecimal(preference.getValue()), property.getLatestSalePrice());
      case DEPOSIT -> subtract(parseBigDecimal(preference.getValue()),
          selectDepositForSorting(property, preferences));
      case MONTHLY_RENT -> subtract(parseBigDecimal(preference.getValue()),
          selectMonthlyRentForSorting(property));
      case AREA -> subtract(property.getExclusiveArea(), parseBigDecimal(preference.getValue()));
      case BUILD_YEAR -> subtract(property.getBuildYear(), parseInteger(preference.getValue()));
      case REGION -> BigDecimal.ONE;
      case BUS -> BigDecimal.valueOf(scoredProperty.surroundingBusCount());
      case SUBWAY -> BigDecimal.valueOf(scoredProperty.surroundingSubwayCount());
      case HOSPITAL -> BigDecimal.valueOf(scoredProperty.surroundingHospitalCount());
      case CCTV -> BigDecimal.valueOf(scoredProperty.surroundingCctvCount());
      case PARK -> BigDecimal.valueOf(scoredProperty.surroundingParkCount());
    };
  }

  private PropertyRecommendationCondition findCondition(
      PropertyRecommendationScore score,
      UserPreferenceResponse preference) {
    return score.getConditions().stream()
        .filter(condition -> condition.getCode() != null
            && preference.getCode() != null
            && condition.getCode().equalsIgnoreCase(preference.getCode()))
        .findFirst()
        .orElse(null);
  }

  private UserPreferenceType parsePreferenceType(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    try {
      return UserPreferenceType.fromCode(code.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private BigDecimal parseBigDecimal(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Integer parseInteger(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private BigDecimal subtract(BigDecimal left, Long right) {
    if (left == null || right == null) {
      return BigDecimal.valueOf(Long.MIN_VALUE);
    }
    return left.subtract(BigDecimal.valueOf(right));
  }

  private BigDecimal subtract(BigDecimal left, BigDecimal right) {
    if (left == null || right == null) {
      return BigDecimal.valueOf(Long.MIN_VALUE);
    }
    return left.subtract(right);
  }

  private BigDecimal subtract(Integer left, Integer right) {
    if (left == null || right == null) {
      return BigDecimal.valueOf(Long.MIN_VALUE);
    }
    return BigDecimal.valueOf(left - right);
  }

  private Long selectDepositForSorting(
      PropertyRecommendationCandidate property,
      List<UserPreferenceResponse> preferences) {
    if (hasPreference(preferences, UserPreferenceType.MONTHLY_RENT)) {
      if (property.getLatestMonthlyRentAmount() != null
          && property.getLatestMonthlyRentAmount() > 0
          && property.getLatestMonthlyRentDeposit() != null) {
        return property.getLatestMonthlyRentDeposit();
      }
      return null;
    }
    return property.getLatestDeposit();
  }

  private Long selectMonthlyRentForSorting(PropertyRecommendationCandidate property) {
    if (property.getLatestMonthlyRentAmount() != null
        && property.getLatestMonthlyRentAmount() > 0) {
      return property.getLatestMonthlyRentAmount();
    }
    return property.getLatestMonthlyRent();
  }

  private boolean hasPreference(List<UserPreferenceResponse> preferences, UserPreferenceType type) {
    return preferences.stream()
        .anyMatch(preference -> preference.getCode() != null
            && preference.getCode().equalsIgnoreCase(type.name()));
  }

  private PropertyRecommendationResponse toResponse(
      PropertyRecommendationCandidate property,
      PropertyRecommendationScore score) {
    return new PropertyRecommendationResponse(
        property.getId(),
        property.getPropertyType(),
        property.getName(),
        property.getSggCd(),
        property.getUmdNm(),
        property.getJibun(),
        property.getBuildYear(),
        property.getLatitude(),
        property.getLongitude(),
        property.getLatestSalePrice(),
        property.getLatestDeposit(),
        property.getLatestMonthlyRent(),
        property.getLatestMonthlyRentDeposit(),
        property.getLatestDealPrice(),
        property.getLatestDealDate(),
        property.getExclusiveArea(),
        score.getRecommendationStatus(),
        score.getScore()
    );
  }

  private boolean isPositiveScore(PropertyRecommendationScore score) {
    return score.getScore() != null && score.getScore() > 0;
  }

  private int scoreValue(PropertyRecommendationScore score) {
    return score.getScore() == null ? 0 : score.getScore();
  }

  private record ScoredProperty(
      PropertyRecommendationCandidate candidate,
      PropertyRecommendationScore score,
      SurroundingSummaryResponse surroundingSummary
  ) {

    int surroundingBusCount() {
      return countMatchedFacility("BUS", surroundingSummary == null ? 0 : surroundingSummary.getBusCount());
    }

    int surroundingSubwayCount() {
      return countMatchedFacility("SUBWAY",
          surroundingSummary == null ? 0 : surroundingSummary.getSubwayCount());
    }

    int surroundingHospitalCount() {
      return countMatchedFacility("HOSPITAL",
          surroundingSummary == null ? 0 : surroundingSummary.getHospitalCount());
    }

    int surroundingCctvCount() {
      return countMatchedFacility("CCTV", surroundingSummary == null ? 0 : surroundingSummary.getCctvCount());
    }

    int surroundingParkCount() {
      return countMatchedFacility("PARK", surroundingSummary == null ? 0 : surroundingSummary.getParkCount());
    }

    private int countMatchedFacility(String code, int count) {
      boolean matched = score.getConditions().stream()
          .filter(condition -> code.equalsIgnoreCase(condition.getCode()) && condition.isMatched())
          .findFirst()
          .isPresent();
      return matched ? count : 0;
    }
  }
}
