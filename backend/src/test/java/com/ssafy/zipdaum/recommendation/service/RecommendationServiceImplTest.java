package com.ssafy.zipdaum.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.dto.SurroundingResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecommendationServiceImplTest {

  private final RecommendationMapper recommendationMapper = mock(RecommendationMapper.class);
  private final UserPreferenceService userPreferenceService = mock(UserPreferenceService.class);
  private final SurroundingService surroundingService = mock(SurroundingService.class);
  private final RecommendationScoreService recommendationScoreService =
      mock(RecommendationScoreService.class);
  private final RecommendationServiceImpl service = new RecommendationServiceImpl(
      recommendationMapper,
      userPreferenceService,
      surroundingService,
      recommendationScoreService
  );

  @Test
  void findPropertyRecommendationScore_주택과_맞춤조건으로_점수를_계산한다() {
    PropertyRecommendationCandidate property = property();
    List<UserPreferenceResponse> preferences = List.of(preference("SUBWAY", "true"));
    SurroundingSummaryResponse summary = new SurroundingSummaryResponse(1, 2, 0, 0, 0);
    SurroundingResponse surroundings = new SurroundingResponse(
        BigDecimal.valueOf(35.1),
        BigDecimal.valueOf(129.1),
        1000,
        summary,
        List.of()
    );
    PropertyRecommendationScore score = new PropertyRecommendationScore(100, 1, 1, List.of(), List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(10L)).willReturn(property);
    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(surroundingService.findSurroundings(
        BigDecimal.valueOf(35.1),
        BigDecimal.valueOf(129.1),
        1000
    )).willReturn(surroundings);
    given(recommendationScoreService.calculateMatchScore(property, preferences, summary))
        .willReturn(score);

    PropertyRecommendationScore result = service.findPropertyRecommendationScore(1L, 10L);

    assertThat(result).isSameAs(score);
  }

  @Test
  void findPropertyRecommendationScore_주택이_없으면_PROPERTY_NOT_FOUND_예외가_발생한다() {
    given(recommendationMapper.selectPropertyRecommendationCandidate(10L)).willReturn(null);

    assertThatThrownBy(() -> service.findPropertyRecommendationScore(1L, 10L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
  }

  @Test
  void findPropertyRecommendationScore_시설_조건이_없으면_주변시설을_조회하지_않는다() {
    PropertyRecommendationCandidate property = property();
    List<UserPreferenceResponse> preferences = List.of(preference("DEPOSIT", "300000000"));
    PropertyRecommendationScore score = new PropertyRecommendationScore(100, 1, 1, List.of(), List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(10L)).willReturn(property);
    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationScoreService.calculateMatchScore(property, preferences, null))
        .willReturn(score);

    PropertyRecommendationScore result = service.findPropertyRecommendationScore(1L, 10L);

    assertThat(result).isSameAs(score);
    then(surroundingService).shouldHaveNoInteractions();
  }

  @Test
  void findPropertyRecommendationScore_좌표가_없으면_시설조건은_불일치로_계산한다() {
    PropertyRecommendationCandidate property = property();
    property.setLatitude(null);
    property.setLongitude(null);
    List<UserPreferenceResponse> preferences = List.of(preference("PARK", "true"));
    PropertyRecommendationScore score = new PropertyRecommendationScore(0, 1, 0, List.of(), List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(10L)).willReturn(property);
    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationScoreService.calculateMatchScore(property, preferences, null))
        .willReturn(score);

    PropertyRecommendationScore result = service.findPropertyRecommendationScore(1L, 10L);

    assertThat(result).isSameAs(score);
  }

  @Test
  void findPropertyRecommendations_우선순위가_높은_매매가조건의_여유금액이_큰_주택을_먼저_반환한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    PropertyRecommendationCandidate cheapProperty = property(1L, 50_000_000L);
    PropertyRecommendationCandidate expensiveProperty = property(2L, 70_000_000L);
    cheapProperty.setLatestSalePrice(50_000_000L);
    expensiveProperty.setLatestSalePrice(70_000_000L);
    List<UserPreferenceResponse> preferences = List.of(preference("SALE_PRICE", "100000000"));

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates())
        .willReturn(List.of(expensiveProperty, cheapProperty));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(1L, 2L);
  }

  @Test
  void findPropertyRecommendations_월세조건이_있으면_월세거래_보증금_여유금액으로_정렬한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    PropertyRecommendationCandidate lowerMonthlyDepositProperty = property(1L, 300_000_000L);
    lowerMonthlyDepositProperty.setLatestMonthlyRentDeposit(20_000_000L);
    lowerMonthlyDepositProperty.setLatestMonthlyRentAmount(700_000L);
    PropertyRecommendationCandidate higherMonthlyDepositProperty = property(2L, 100_000_000L);
    higherMonthlyDepositProperty.setLatestMonthlyRentDeposit(30_000_000L);
    higherMonthlyDepositProperty.setLatestMonthlyRentAmount(700_000L);
    List<UserPreferenceResponse> preferences = List.of(
        preference("DEPOSIT", "40000000"),
        preference("MONTHLY_RENT", "800000", 2)
    );

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates())
        .willReturn(List.of(higherMonthlyDepositProperty, lowerMonthlyDepositProperty));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(1L, 2L);
  }

  private PropertyRecommendationCandidate property() {
    return property(10L, 300_000_000L);
  }

  private PropertyRecommendationCandidate property(Long id, Long latestDealPrice) {
    PropertyRecommendationCandidate property = new PropertyRecommendationCandidate();
    property.setId(id);
    property.setSggCd("26110");
    property.setUmdNm("우동");
    property.setBuildYear(2020);
    property.setLatitude(BigDecimal.valueOf(35.1));
    property.setLongitude(BigDecimal.valueOf(129.1));
    property.setLatestDeposit(latestDealPrice);
    property.setLatestDealPrice(latestDealPrice);
    property.setExclusiveArea(new BigDecimal("84.5"));
    return property;
  }

  private UserPreferenceResponse preference(String code, String value) {
    UserPreferenceResponse preference = new UserPreferenceResponse();
    preference.setCode(code);
    preference.setValue(value);
    preference.setPriority(1);
    return preference;
  }

  private UserPreferenceResponse preference(String code, String value, Integer priority) {
    UserPreferenceResponse preference = preference(code, value);
    preference.setPriority(priority);
    return preference;
  }
}
