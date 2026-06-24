package com.ssafy.zipdaum.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidateFilter;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.dto.RecommendationStatus;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    PropertyRecommendationScore score = new PropertyRecommendationScore(100, List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L)).willReturn(property);
    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(surroundingService.findRecommendationSurroundingSummary(
        BigDecimal.valueOf(35.1),
        BigDecimal.valueOf(129.1)
    )).willReturn(summary);
    given(recommendationScoreService.calculateMatchScore(property, preferences, summary))
        .willReturn(score);

    PropertyRecommendationScore result = service.findPropertyRecommendationScore(1L, 10L);

    assertThat(result).isSameAs(score);
  }

  @Test
  void findPropertyRecommendationScore_행동로그가_있어도_평가불가_상태를_유지한다() {
    PropertyRecommendationCandidate property = property();
    property.setInteractionViewCount(3);
    List<UserPreferenceResponse> preferences = List.of(preference("SUBWAY", "false"));
    PropertyRecommendationScore score = new PropertyRecommendationScore(
        RecommendationStatus.NO_EVALUABLE_CONDITION,
        null,
        List.of()
    );

    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L)).willReturn(property);
    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationScoreService.calculateMatchScore(property, preferences, null))
        .willReturn(score);

    PropertyRecommendationScore result = service.findPropertyRecommendationScore(1L, 10L);

    assertThat(result).isSameAs(score);
  }

  @Test
  void findPropertyRecommendationScore_주택이_없으면_PROPERTY_NOT_FOUND_예외가_발생한다() {
    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L)).willReturn(null);

    assertThatThrownBy(() -> service.findPropertyRecommendationScore(1L, 10L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
  }

  @Test
  void findPropertyRecommendationScore_시설_조건이_없으면_주변시설을_조회하지_않는다() {
    PropertyRecommendationCandidate property = property();
    List<UserPreferenceResponse> preferences = List.of(preference("DEPOSIT", "300000000"));
    PropertyRecommendationScore score = new PropertyRecommendationScore(100, List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L)).willReturn(property);
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
    PropertyRecommendationScore score = new PropertyRecommendationScore(0, List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L)).willReturn(property);
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
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
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
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(higherMonthlyDepositProperty, lowerMonthlyDepositProperty));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(1L, 2L);
  }

  @Test
  void findPropertyRecommendations_최종점수가_높은_주택을_먼저_반환한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    PropertyRecommendationCandidate lowerScoreProperty = property(1L, 80_000_000L);
    PropertyRecommendationCandidate higherScoreProperty = property(2L, 100_000_000L);
    lowerScoreProperty.setLatestSalePrice(109_000_000L);
    lowerScoreProperty.setExclusiveArea(new BigDecimal("70.0"));
    higherScoreProperty.setLatestSalePrice(100_000_000L);
    higherScoreProperty.setExclusiveArea(new BigDecimal("84.5"));
    List<UserPreferenceResponse> preferences = List.of(
        preference("SALE_PRICE", "100000000"),
        preference("AREA", "84.5", 2)
    );

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(lowerScoreProperty, higherScoreProperty));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(2L, 1L);
  }

  @Test
  void findPropertyRecommendations_시설조건은_실제_주변시설_개수가_많은_주택을_먼저_반환한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    PropertyRecommendationCandidate fewSubwayProperty = property(1L, 300_000_000L);
    PropertyRecommendationCandidate manySubwayProperty = property(2L, 300_000_000L);
    manySubwayProperty.setLatitude(BigDecimal.valueOf(35.2));
    manySubwayProperty.setLongitude(BigDecimal.valueOf(129.2));
    List<UserPreferenceResponse> preferences = List.of(preference("SUBWAY", "true"));

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(fewSubwayProperty, manySubwayProperty));
    given(surroundingService.findRecommendationSurroundingSummary(
        BigDecimal.valueOf(35.1),
        BigDecimal.valueOf(129.1)
    )).willReturn(new SurroundingSummaryResponse(0, 1, 0, 0, 0));
    given(surroundingService.findRecommendationSurroundingSummary(
        BigDecimal.valueOf(35.2),
        BigDecimal.valueOf(129.2)
    )).willReturn(new SurroundingSummaryResponse(0, 3, 0, 0, 0));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(2L, 1L);
  }

  @Test
  void findPropertyRecommendations_평가가능한_조건과_추천_이력이_없으면_빈_목록을_반환한다() {
    List<UserPreferenceResponse> preferences = List.of(
        preference("SUBWAY", "false"),
        preference("PARK", "false", 2)
    );

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any())).willReturn(List.of());

    List<PropertyRecommendationResponse> result = service.findPropertyRecommendations(1L);

    assertThat(result).isEmpty();
    then(recommendationMapper).should().selectPropertyRecommendationCandidates(any());
    then(surroundingService).shouldHaveNoInteractions();
    then(recommendationScoreService).shouldHaveNoInteractions();
  }

  @Test
  void findPropertyRecommendations_평가가능한_조건이_없어도_행동로그_조회횟수로_추천한다() {
    List<UserPreferenceResponse> preferences = List.of(preference("SUBWAY", "false"));
    PropertyRecommendationCandidate property = property();
    property.setInteractionViewCount(3);

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(property));

    List<PropertyRecommendationResponse> result = service.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId, PropertyRecommendationResponse::getScore)
        .containsExactly(tuple(10L, 15));
    then(recommendationScoreService).shouldHaveNoInteractions();
    then(surroundingService).shouldHaveNoInteractions();
  }

  @Test
  void findPropertyRecommendations_맞춤조건을_SQL_후보필터로_전달한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    List<UserPreferenceResponse> preferences = List.of(
        preference("REGION", " 부산광역시 해운대구 우동 ", 1),
        preference("SALE_PRICE", "100000000", 2),
        preference("DEPOSIT", "40000000", 3),
        preference("MONTHLY_RENT", "800000", 4),
        preference("AREA", "84.5", 5)
    );

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of());

    serviceWithRealScore.findPropertyRecommendations(1L);

    ArgumentCaptor<PropertyRecommendationCandidateFilter> captor =
        ArgumentCaptor.forClass(PropertyRecommendationCandidateFilter.class);
    then(recommendationMapper).should().selectPropertyRecommendationCandidates(captor.capture());
    PropertyRecommendationCandidateFilter filter = captor.getValue();
    assertThat(filter.getUserId()).isEqualTo(1L);
    assertThat(filter.getRegions()).containsExactly("부산광역시 해운대구 우동");
    assertThat(filter.getSalePriceMax()).isEqualTo(120_000_000L);
    assertThat(filter.getDepositMax()).isEqualTo(48_000_000L);
    assertThat(filter.getMonthlyRentMax()).isEqualTo(960_000L);
    assertThat(filter.getMinExclusiveArea()).isEqualByComparingTo(new BigDecimal("67.60"));
    assertThat(filter.isMonthlyRentPreferred()).isTrue();
  }

  @Test
  void findPropertyRecommendations_행동로그_조회횟수_가산점을_추천목록_정렬에_반영한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    PropertyRecommendationCandidate viewedProperty = property(1L, 100_000_000L);
    PropertyRecommendationCandidate otherProperty = property(2L, 100_000_000L);
    viewedProperty.setLatestSalePrice(108_000_000L);
    viewedProperty.setInteractionViewCount(1);
    otherProperty.setLatestSalePrice(108_000_000L);
    List<UserPreferenceResponse> preferences = List.of(preference("SALE_PRICE", "100000000"));

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(otherProperty, viewedProperty));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(1L, 2L);
    assertThat(result)
        .extracting(PropertyRecommendationResponse::getScore)
        .containsExactly(65, 60);
  }

  @Test
  void findPropertyRecommendations_행동로그_가산점을_추천목록_정렬에_반영한다() {
    RecommendationServiceImpl serviceWithRealScore = new RecommendationServiceImpl(
        recommendationMapper,
        userPreferenceService,
        surroundingService,
        new RecommendationScoreServiceImpl()
    );
    PropertyRecommendationCandidate interactedProperty = property(1L, 100_000_000L);
    PropertyRecommendationCandidate otherProperty = property(2L, 100_000_000L);
    interactedProperty.setLatestSalePrice(108_000_000L);
    otherProperty.setLatestSalePrice(108_000_000L);
    interactedProperty.setInteractionViewCount(1);
    interactedProperty.setInteractionTotalDwellTimeMillis(30_000L);
    interactedProperty.setInteractionMaxScrollDepthPercent(80);
    interactedProperty.setRecommendationDetailClickCount(1);
    interactedProperty.setDealHistoryClickCount(1);
    List<UserPreferenceResponse> preferences = List.of(preference("SALE_PRICE", "100000000"));

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(otherProperty, interactedProperty));

    List<PropertyRecommendationResponse> result =
        serviceWithRealScore.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId)
        .containsExactly(1L, 2L);
    assertThat(result)
        .extracting(PropertyRecommendationResponse::getScore)
        .containsExactly(85, 60);
  }

  @Test
  void findPropertyRecommendations_평가가능한_조건이_없어도_행동로그로_추천한다() {
    List<UserPreferenceResponse> preferences = List.of(preference("SUBWAY", "false"));
    PropertyRecommendationCandidate interactedProperty = property();
    interactedProperty.setInteractionViewCount(1);
    interactedProperty.setInteractionTotalDwellTimeMillis(30_000L);
    interactedProperty.setInteractionMaxScrollDepthPercent(80);

    given(userPreferenceService.findPreferences(1L)).willReturn(preferences);
    given(recommendationMapper.selectPropertyRecommendationCandidates(any()))
        .willReturn(List.of(interactedProperty));

    List<PropertyRecommendationResponse> result = service.findPropertyRecommendations(1L);

    assertThat(result)
        .extracting(PropertyRecommendationResponse::getId, PropertyRecommendationResponse::getScore)
        .containsExactly(tuple(10L, 15));
    then(recommendationScoreService).shouldHaveNoInteractions();
    then(surroundingService).shouldHaveNoInteractions();
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
