package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.domain.UserPreferenceType;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCondition;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

  private final RecommendationMapper recommendationMapper;
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

    log.info("주택 맞춤 조건 적합도 계산 완료 score={}, evaluatedCount={}, matchedCount={}",
        score.getScore(), score.getEvaluatedCount(), score.getMatchedCount());
    return score;
  }

  @Override
  @Transactional(readOnly = true)
  public List<PropertyRecommendationResponse> findPropertyRecommendations(Long userId) {
    List<UserPreferenceResponse> preferences = userPreferenceService.findPreferences(userId);
    List<PropertyRecommendationCandidate> candidates =
        recommendationMapper.selectPropertyRecommendationCandidates();

    List<ScoredProperty> scoredProperties = new ArrayList<>();
    for (PropertyRecommendationCandidate candidate : candidates) {
      SurroundingSummaryResponse surroundingSummary = findSurroundingSummary(candidate, preferences);
      PropertyRecommendationScore score = recommendationScoreService.calculateMatchScore(
          candidate,
          preferences,
          surroundingSummary
      );
      if (score.getScore() > 0) {
        scoredProperties.add(new ScoredProperty(candidate, score));
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

    return surroundingService.findSurroundings(
        property.getLatitude(),
        property.getLongitude(),
        RECOMMENDATION_SURROUNDING_RADIUS_METERS
    ).getSummary();
  }

  private boolean hasFacilityPreference(List<UserPreferenceResponse> preferences) {
    return preferences.stream()
        .anyMatch(preference ->
            isFacilityCode(preference.getCode()) && "true".equalsIgnoreCase(preference.getValue())
        );
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

      int scoreCompared = Integer.compare(right.score().getScore(), left.score().getScore());
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
        property.getLatestMonthlyRentAmount(),
        property.getLatestDealPrice(),
        property.getLatestDealDate(),
        property.getExclusiveArea(),
        score.getScore(),
        score.getEvaluatedCount(),
        score.getMatchedCount(),
        score.getMatchedReasons(),
        score.getConditions()
    );
  }

  private record ScoredProperty(
      PropertyRecommendationCandidate candidate,
      PropertyRecommendationScore score
  ) {

    int surroundingBusCount() {
      return countMatchedFacility("BUS");
    }

    int surroundingSubwayCount() {
      return countMatchedFacility("SUBWAY");
    }

    int surroundingHospitalCount() {
      return countMatchedFacility("HOSPITAL");
    }

    int surroundingCctvCount() {
      return countMatchedFacility("CCTV");
    }

    int surroundingParkCount() {
      return countMatchedFacility("PARK");
    }

    private int countMatchedFacility(String code) {
      return score.getConditions().stream()
          .filter(condition -> code.equalsIgnoreCase(condition.getCode()) && condition.isMatched())
          .mapToInt(condition -> condition.getScore())
          .findFirst()
          .orElse(0);
    }
  }
}
