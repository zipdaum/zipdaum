package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.preference.domain.UserPreferenceType;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCondition;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationScoreServiceImpl implements RecommendationScoreService {

  private static final int FULL_MATCH_SCORE = 100;
  private static final int PARTIAL_MATCH_SCORE = 70;

  @Override
  public PropertyRecommendationScore calculateMatchScore(
      PropertyRecommendationCandidate property,
      List<UserPreferenceResponse> preferences,
      SurroundingSummaryResponse surroundingSummary) {
    if (preferences == null || preferences.isEmpty()) {
      return new PropertyRecommendationScore(0, 0, 0, List.of(), List.of());
    }

    List<ScoredPreference> scoredPreferences = preferences.stream()
        .sorted(Comparator.comparing(
            UserPreferenceResponse::getPriority,
            Comparator.nullsLast(Comparator.naturalOrder())
        ))
        .map(preference -> scorePreference(property, preference, preferences, surroundingSummary))
        .toList();

    if (scoredPreferences.isEmpty()) {
      return new PropertyRecommendationScore(0, 0, 0, List.of(), List.of());
    }

    int maxPriority = scoredPreferences.stream()
        .map(ScoredPreference::priority)
        .filter(priority -> priority != null && priority > 0)
        .max(Comparator.naturalOrder())
        .orElse(1);

    int weightedScoreSum = 0;
    int weightSum = 0;
    int matchedCount = 0;
    List<String> matchedReasons = new ArrayList<>();
    List<PropertyRecommendationCondition> conditions = new ArrayList<>();

    for (ScoredPreference scoredPreference : scoredPreferences) {
      int weight = calculateWeight(scoredPreference.priority(), maxPriority);
      weightedScoreSum += scoredPreference.score() * weight;
      weightSum += weight;

      if (scoredPreference.score() > 0) {
        matchedCount++;
      }
      if (scoredPreference.score() == FULL_MATCH_SCORE && scoredPreference.reason() != null) {
        matchedReasons.add(scoredPreference.reason());
      }
      conditions.add(toCondition(scoredPreference));
    }

    int score = Math.round((float) weightedScoreSum / weightSum);
    return new PropertyRecommendationScore(
        score,
        scoredPreferences.size(),
        matchedCount,
        matchedReasons,
        conditions
    );
  }

  private ScoredPreference scorePreference(
      PropertyRecommendationCandidate property,
      UserPreferenceResponse preference,
      List<UserPreferenceResponse> preferences,
      SurroundingSummaryResponse surroundingSummary) {
    UserPreferenceType type = parseType(preference.getCode());
    if (type == null) {
      return unmatched(preference, null);
    }

    return switch (type) {
      case SALE_PRICE -> scorePrice(
          property.getLatestSalePrice(),
          preference,
          type,
          "매매가 조건과 적합");
      case DEPOSIT -> scorePrice(
          selectDepositForScoring(property, preferences),
          preference,
          type,
          "보증금 조건과 적합");
      case MONTHLY_RENT -> scoreMonthlyRent(property, preference, preferences);
      case AREA -> scoreArea(property, preference);
      case BUILD_YEAR -> scoreBuildYear(property, preference);
      case REGION -> scoreRegion(property, preference);
      case BUS -> scoreFacility(preference, surroundingSummary, surroundingSummary == null
          ? 0 : surroundingSummary.getBusCount(), "버스 정류장이 가까움");
      case SUBWAY -> scoreFacility(preference, surroundingSummary, surroundingSummary == null
          ? 0 : surroundingSummary.getSubwayCount(), "지하철역이 가까움");
      case HOSPITAL -> scoreFacility(preference, surroundingSummary, surroundingSummary == null
          ? 0 : surroundingSummary.getHospitalCount(), "병원이 가까움");
      case CCTV -> scoreFacility(preference, surroundingSummary, surroundingSummary == null
          ? 0 : surroundingSummary.getCctvCount(), "방범용 CCTV가 가까움");
      case PARK -> scoreFacility(preference, surroundingSummary, surroundingSummary == null
          ? 0 : surroundingSummary.getParkCount(), "공원이 가까움");
    };
  }

  private ScoredPreference scorePrice(
      Long actualPrice,
      UserPreferenceResponse preference,
      UserPreferenceType type,
      String reason) {
    Long preferredPrice = parseLong(preference.getValue());
    if (preferredPrice == null || actualPrice == null || actualPrice <= 0) {
      return unmatched(preference, type);
    }

    if (actualPrice <= preferredPrice) {
      return scored(preference, type, FULL_MATCH_SCORE, reason);
    }
    if (actualPrice <= preferredPrice * 1.1) {
      return scored(preference, type, PARTIAL_MATCH_SCORE, null);
    }
    return scored(preference, type, 0, null);
  }

  private ScoredPreference scoreMonthlyRent(
      PropertyRecommendationCandidate property,
      UserPreferenceResponse preference,
      List<UserPreferenceResponse> preferences) {
    Long preferredMonthlyRent = parseLong(preference.getValue());
    Long actualMonthlyRent = property.getLatestMonthlyRentAmount();
    if (actualMonthlyRent == null) {
      actualMonthlyRent = property.getLatestMonthlyRent();
    }
    if (preferredMonthlyRent == null || actualMonthlyRent == null || actualMonthlyRent <= 0) {
      return unmatched(preference, UserPreferenceType.MONTHLY_RENT);
    }

    int monthlyRentScore = calculatePriceScore(actualMonthlyRent, preferredMonthlyRent);
    UserPreferenceResponse depositPreference = findPreference(preferences, UserPreferenceType.DEPOSIT);
    if (depositPreference == null) {
      return scoredMonthlyRent(preference, monthlyRentScore);
    }

    Long preferredDeposit = parseLong(depositPreference.getValue());
    Long actualDeposit = property.getLatestMonthlyRentDeposit();
    if (preferredDeposit == null || actualDeposit == null) {
      return unmatched(preference, UserPreferenceType.MONTHLY_RENT);
    }

    int depositScore = calculatePriceScore(actualDeposit, preferredDeposit);
    return scoredMonthlyRent(preference, Math.min(monthlyRentScore, depositScore));
  }

  private ScoredPreference scoredMonthlyRent(UserPreferenceResponse preference, int score) {
    String reason = score == FULL_MATCH_SCORE ? "보증금/월세 조건과 적합" : null;
    return scored(preference, UserPreferenceType.MONTHLY_RENT, score, reason);
  }

  private int calculatePriceScore(Long actualPrice, Long preferredPrice) {
    if (actualPrice <= preferredPrice) {
      return FULL_MATCH_SCORE;
    }
    if (actualPrice <= preferredPrice * 1.1) {
      return PARTIAL_MATCH_SCORE;
    }
    return 0;
  }

  private Long selectDepositForScoring(
      PropertyRecommendationCandidate property,
      List<UserPreferenceResponse> preferences) {
    if (findPreference(preferences, UserPreferenceType.MONTHLY_RENT) != null) {
      if (property.getLatestMonthlyRentDeposit() != null
          && property.getLatestMonthlyRentAmount() != null
          && property.getLatestMonthlyRentAmount() > 0) {
        return property.getLatestMonthlyRentDeposit();
      }
      return null;
    }
    return property.getLatestDeposit();
  }

  private ScoredPreference scoreArea(
      PropertyRecommendationCandidate property,
      UserPreferenceResponse preference) {
    BigDecimal preferredArea = parseBigDecimal(preference.getValue());
    BigDecimal exclusiveArea = property.getExclusiveArea();
    if (preferredArea == null || exclusiveArea == null) {
      return unmatched(preference, UserPreferenceType.AREA);
    }

    if (exclusiveArea.compareTo(preferredArea) >= 0) {
      return scored(preference, UserPreferenceType.AREA, FULL_MATCH_SCORE, "희망 면적과 일치");
    }
    if (exclusiveArea.compareTo(preferredArea.multiply(BigDecimal.valueOf(0.9))) >= 0) {
      return scored(preference, UserPreferenceType.AREA, PARTIAL_MATCH_SCORE, null);
    }
    return scored(preference, UserPreferenceType.AREA, 0, null);
  }

  private ScoredPreference scoreBuildYear(
      PropertyRecommendationCandidate property,
      UserPreferenceResponse preference) {
    Integer preferredBuildYear = parseInteger(preference.getValue());
    Integer buildYear = property.getBuildYear();
    if (preferredBuildYear == null || buildYear == null) {
      return unmatched(preference, UserPreferenceType.BUILD_YEAR);
    }

    if (buildYear >= preferredBuildYear) {
      return scored(preference, UserPreferenceType.BUILD_YEAR, FULL_MATCH_SCORE, "희망 건축연도와 일치");
    }
    if (buildYear >= preferredBuildYear - 5) {
      return scored(preference, UserPreferenceType.BUILD_YEAR, PARTIAL_MATCH_SCORE, null);
    }
    return scored(preference, UserPreferenceType.BUILD_YEAR, 0, null);
  }

  private ScoredPreference scoreRegion(
      PropertyRecommendationCandidate property,
      UserPreferenceResponse preference) {
    String preferredRegion = normalize(preference.getValue());
    if (preferredRegion == null) {
      return unmatched(preference, UserPreferenceType.REGION);
    }

    String sggCd = normalize(property.getSggCd());
    String umdNm = normalize(property.getUmdNm());
    boolean matched = preferredRegion.equals(sggCd)
        || (umdNm != null && preferredRegion.contains(umdNm))
        || (umdNm != null && umdNm.contains(preferredRegion));
    return scored(
        preference,
        UserPreferenceType.REGION,
        matched ? FULL_MATCH_SCORE : 0,
        matched ? "선호 지역과 일치" : null
    );
  }

  private ScoredPreference scoreFacility(
      UserPreferenceResponse preference,
      SurroundingSummaryResponse surroundingSummary,
      int count,
      String reason) {
    Boolean required = parseBoolean(preference.getValue());
    if (required == null || !required || surroundingSummary == null) {
      return unmatched(preference, parseType(preference.getCode()));
    }

    return scored(
        preference,
        parseType(preference.getCode()),
        count > 0 ? FULL_MATCH_SCORE : 0,
        count > 0 ? reason : null
    );
  }

  private ScoredPreference scored(
      UserPreferenceResponse preference,
      UserPreferenceType type,
      int score,
      String reason) {
    return new ScoredPreference(
        preference.getCode(),
        resolveName(preference, type),
        preference.getValue(),
        preference.getPriority(),
        score,
        reason
    );
  }

  private ScoredPreference unmatched(UserPreferenceResponse preference, UserPreferenceType type) {
    return scored(preference, type, 0, null);
  }

  private String resolveName(UserPreferenceResponse preference, UserPreferenceType type) {
    if (preference.getName() != null && !preference.getName().isBlank()) {
      return preference.getName();
    }
    if (type != null) {
      return type.getDisplayName();
    }
    return preference.getCode();
  }

  private PropertyRecommendationCondition toCondition(ScoredPreference scoredPreference) {
    return new PropertyRecommendationCondition(
        scoredPreference.code(),
        scoredPreference.name(),
        scoredPreference.value(),
        scoredPreference.priority(),
        scoredPreference.score() > 0,
        scoredPreference.score(),
        scoredPreference.reason()
    );
  }

  private UserPreferenceType parseType(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    try {
      return UserPreferenceType.fromCode(code.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private UserPreferenceResponse findPreference(
      List<UserPreferenceResponse> preferences,
      UserPreferenceType type) {
    return preferences.stream()
        .filter(preference -> preference.getCode() != null
            && preference.getCode().equalsIgnoreCase(type.name()))
        .findFirst()
        .orElse(null);
  }

  private int calculateWeight(Integer priority, int maxPriority) {
    if (priority == null || priority < 1) {
      return 1;
    }
    return Math.max(maxPriority - priority + 1, 1);
  }

  private String normalize(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private Long parseLong(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(value);
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

  private Boolean parseBoolean(String value) {
    if ("true".equalsIgnoreCase(value)) {
      return true;
    }
    if ("false".equalsIgnoreCase(value)) {
      return false;
    }
    return null;
  }

  private record ScoredPreference(
      String code,
      String name,
      String value,
      Integer priority,
      int score,
      String reason
  ) {
  }
}
