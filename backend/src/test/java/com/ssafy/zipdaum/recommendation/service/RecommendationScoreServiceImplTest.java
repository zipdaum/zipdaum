package com.ssafy.zipdaum.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationScoreServiceImplTest {

  private final RecommendationScoreServiceImpl service = new RecommendationScoreServiceImpl();

  @Test
  void calculateMatchScore_매매가_지역_주변시설을_가중치로_점수화한다() {
    PropertyRecommendationCandidate property = property(
        "26110",
        "우동",
        2018,
        450_000_000L,
        0L,
        0L,
        null
    );
    SurroundingSummaryResponse surroundingSummary = new SurroundingSummaryResponse(3, 1, 0, 2, 0);

    PropertyRecommendationScore result = service.calculateMatchScore(
        property,
        List.of(
            preference("SALE_PRICE", "400000000", 1),
            preference("REGION", "부산광역시 해운대구 우동", 2),
            preference("SUBWAY", "true", 3)
        ),
        surroundingSummary
    );

    assertThat(result.getScore()).isEqualTo(50);
    assertThat(result.getEvaluatedCount()).isEqualTo(3);
    assertThat(result.getMatchedCount()).isEqualTo(2);
    assertThat(result.getMatchedReasons())
        .containsExactly("선호 지역과 일치", "지하철역이 가까움");
    assertThat(result.getConditions())
        .extracting("code", "matched", "score")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("SALE_PRICE", false, 0),
            org.assertj.core.groups.Tuple.tuple("REGION", true, 100),
            org.assertj.core.groups.Tuple.tuple("SUBWAY", true, 100)
        );
  }

  @Test
  void calculateMatchScore_전체_조건의_적합여부를_반환한다() {
    PropertyRecommendationCandidate property = property(
        "26110",
        "우동",
        2018,
        0L,
        0L,
        0L,
        new BigDecimal("84.5")
    );

    PropertyRecommendationScore result = service.calculateMatchScore(
        property,
        List.of(
            preference("AREA", "84.5", 1),
            preference("BUS", "true", 2),
            preference("REGION", "우동", 3),
            preference("BUILD_YEAR", "2020", 4),
            preference("PARK", "true", 5)
        ),
        null
    );

    assertThat(result.getScore()).isEqualTo(63);
    assertThat(result.getEvaluatedCount()).isEqualTo(5);
    assertThat(result.getMatchedCount()).isEqualTo(3);
    assertThat(result.getMatchedReasons())
        .containsExactly("희망 면적과 일치", "선호 지역과 일치");
    assertThat(result.getConditions())
        .extracting("code", "matched", "score")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("AREA", true, 100),
            org.assertj.core.groups.Tuple.tuple("BUS", false, 0),
            org.assertj.core.groups.Tuple.tuple("REGION", true, 100),
            org.assertj.core.groups.Tuple.tuple("BUILD_YEAR", true, 70),
            org.assertj.core.groups.Tuple.tuple("PARK", false, 0)
        );
  }

  @Test
  void calculateMatchScore_건축연도는_희망연도보다_오래되면_부분점수를_준다() {
    PropertyRecommendationCandidate property = property(
        "26110",
        "우동",
        2016,
        0L,
        250_000_000L,
        0L,
        null
    );

    PropertyRecommendationScore result = service.calculateMatchScore(
        property,
        List.of(preference("BUILD_YEAR", "2020", 1)),
        null
    );

    assertThat(result.getScore()).isEqualTo(70);
    assertThat(result.getEvaluatedCount()).isEqualTo(1);
    assertThat(result.getMatchedCount()).isEqualTo(1);
    assertThat(result.getMatchedReasons()).isEmpty();
  }

  @Test
  void calculateMatchScore_월세는_월세거래의_보증금과_월세를_함께_판단한다() {
    PropertyRecommendationCandidate property = property(
        "26110",
        "우동",
        2018,
        0L,
        200_000_000L,
        0L,
        null
    );
    property.setLatestMonthlyRentDeposit(30_000_000L);
    property.setLatestMonthlyRentAmount(700_000L);

    PropertyRecommendationScore result = service.calculateMatchScore(
        property,
        List.of(
            preference("DEPOSIT", "40000000", 1),
            preference("MONTHLY_RENT", "800000", 2)
        ),
        null
    );

    assertThat(result.getScore()).isEqualTo(100);
    assertThat(result.getEvaluatedCount()).isEqualTo(2);
    assertThat(result.getMatchedCount()).isEqualTo(2);
    assertThat(result.getMatchedReasons())
        .containsExactly("보증금 조건과 적합", "보증금/월세 조건과 적합");
    assertThat(result.getConditions())
        .extracting("code", "matched", "score")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("DEPOSIT", true, 100),
            org.assertj.core.groups.Tuple.tuple("MONTHLY_RENT", true, 100)
        );
  }

  @Test
  void calculateMatchScore_월세조건이_있으면_전세보증금으로_보증금조건을_만족시키지_않는다() {
    PropertyRecommendationCandidate property = property(
        "26110",
        "우동",
        2018,
        0L,
        30_000_000L,
        0L,
        null
    );
    property.setLatestMonthlyRentDeposit(50_000_000L);
    property.setLatestMonthlyRentAmount(700_000L);

    PropertyRecommendationScore result = service.calculateMatchScore(
        property,
        List.of(
            preference("DEPOSIT", "40000000", 1),
            preference("MONTHLY_RENT", "800000", 2)
        ),
        null
    );

    assertThat(result.getScore()).isZero();
    assertThat(result.getMatchedCount()).isZero();
    assertThat(result.getMatchedReasons()).isEmpty();
    assertThat(result.getConditions())
        .extracting("code", "matched", "score")
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("DEPOSIT", false, 0),
            org.assertj.core.groups.Tuple.tuple("MONTHLY_RENT", false, 0)
        );
  }

  @Test
  void calculateMatchScore_false_시설조건은_평가_대상에서_제외한다() {
    PropertyRecommendationCandidate property = property(
        "26110",
        "우동",
        2018,
        0L,
        0L,
        0L,
        null
    );
    SurroundingSummaryResponse surroundingSummary = new SurroundingSummaryResponse(3, 1, 0, 2, 0);

    PropertyRecommendationScore result = service.calculateMatchScore(
        property,
        List.of(
            preference("SUBWAY", "false", 1),
            preference("PARK", "false", 2)
        ),
        surroundingSummary
    );

    assertThat(result.getScore()).isZero();
    assertThat(result.getEvaluatedCount()).isZero();
    assertThat(result.getMatchedCount()).isZero();
    assertThat(result.getMatchedReasons()).isEmpty();
    assertThat(result.getConditions()).isEmpty();
  }

  private PropertyRecommendationCandidate property(
      String sggCd,
      String umdNm,
      Integer buildYear,
      Long latestSalePrice,
      Long latestDeposit,
      Long latestMonthlyRent,
      BigDecimal exclusiveArea) {
    PropertyRecommendationCandidate property = new PropertyRecommendationCandidate();
    property.setSggCd(sggCd);
    property.setUmdNm(umdNm);
    property.setBuildYear(buildYear);
    property.setLatestSalePrice(latestSalePrice);
    property.setLatestDeposit(latestDeposit);
    property.setLatestMonthlyRent(latestMonthlyRent);
    property.setLatestDealPrice(
        latestSalePrice != null && latestSalePrice > 0 ? latestSalePrice : latestDeposit
    );
    property.setExclusiveArea(exclusiveArea);
    return property;
  }

  private UserPreferenceResponse preference(String code, String value, Integer priority) {
    UserPreferenceResponse preference = new UserPreferenceResponse();
    preference.setCode(code);
    preference.setValue(value);
    preference.setPriority(priority);
    return preference;
  }
}
