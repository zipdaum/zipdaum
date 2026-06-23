package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.zipdaum.favorite.service.FavoritePropertyService;
import com.ssafy.zipdaum.global.ai.GmsOpenAiClient;
import com.ssafy.zipdaum.global.util.RedisUtil;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.dto.PropertyAiComparisonRequest;
import com.ssafy.zipdaum.property.dto.PropertyAiComparisonResponse;
import com.ssafy.zipdaum.property.dto.PropertyDealHistoryResponse;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.recommendation.service.RecommendationService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PropertyAiComparisonServiceImplTest {

  private final PropertyService propertyService = mock(PropertyService.class);
  private final SurroundingService surroundingService = mock(SurroundingService.class);
  private final RecommendationService recommendationService = mock(RecommendationService.class);
  private final UserPreferenceService userPreferenceService = mock(UserPreferenceService.class);
  private final FavoritePropertyService favoritePropertyService = mock(FavoritePropertyService.class);
  private final GmsOpenAiClient gmsOpenAiClient = mock(GmsOpenAiClient.class);
  private final RedisUtil redisUtil = mock(RedisUtil.class);
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final PropertyAiComparisonServiceImpl service = new PropertyAiComparisonServiceImpl(
      propertyService,
      surroundingService,
      recommendationService,
      userPreferenceService,
      favoritePropertyService,
      gmsOpenAiClient,
      objectMapper,
      redisUtil
  );

  @Test
  void compareProperties_캐시가_있으면_AI_API를_호출하지_않고_캐시_응답을_반환한다() throws Exception {
    PropertyAiComparisonResponse cachedResponse = comparisonResponse("A");
    givenComparisonInput();
    given(redisUtil.getData(anyString())).willReturn(objectMapper.writeValueAsString(cachedResponse));

    PropertyAiComparisonResponse result = service.compareProperties(1L, comparisonRequest());

    assertThat(result.getRecommendedProperty()).isEqualTo("A");
    assertThat(result.getOneLineSummary()).isEqualTo("A 추천");
    then(gmsOpenAiClient).should(never()).chatCompletion(anyString(), anyString(), anyString());
    then(redisUtil).should(never()).setDataWithTTL(anyString(), anyString(), eq(3600L));
  }

  @Test
  void compareProperties_Redis_장애가_발생해도_AI_응답을_반환한다() {
    givenComparisonInput();
    willThrow(new RuntimeException("redis down")).given(redisUtil).getData(anyString());
    willThrow(new RuntimeException("redis down")).given(redisUtil)
        .setDataWithTTL(anyString(), anyString(), eq(3600L));
    given(gmsOpenAiClient.chatCompletion(anyString(), anyString(), eq("property comparison")))
        .willReturn("""
            {
              "oneLineSummary": "B 추천",
              "recommendedProperty": "B",
              "recommendationReason": "B가 더 적합합니다."
            }
            """);

    PropertyAiComparisonResponse result = service.compareProperties(1L, comparisonRequest());

    assertThat(result.getRecommendedProperty()).isEqualTo("B");
    assertThat(result.getOneLineSummary()).isEqualTo("B 추천");
    then(gmsOpenAiClient).should().chatCompletion(anyString(), anyString(), eq("property comparison"));
  }

  private void givenComparisonInput() {
    given(userPreferenceService.findPreferences(1L)).willReturn(List.of());
    given(favoritePropertyService.findFavoriteProperties(1L)).willReturn(List.of());
    given(propertyService.findPropertyDetail(10L)).willReturn(propertyDetail(10L, "A 주택"));
    given(propertyService.findPropertyDetail(20L)).willReturn(propertyDetail(20L, "B 주택"));
    given(propertyService.findPropertyDealHistories(10L, "JEONSE", 1, 1, 3))
        .willReturn(emptyHistories());
    given(propertyService.findPropertyDealHistories(20L, "JEONSE", 1, 1, 3))
        .willReturn(emptyHistories());
  }

  private PropertyAiComparisonRequest comparisonRequest() {
    PropertyAiComparisonRequest request = new PropertyAiComparisonRequest();
    request.setPropertyIds(List.of(10L, 20L));
    request.setComparisonPurpose("실거주 관점 비교");
    return request;
  }

  private PropertyDetailResponse propertyDetail(Long id, String name) {
    PropertyDetailResponse response = new PropertyDetailResponse();
    response.setId(id);
    response.setName(name);
    response.setPropertyType("APARTMENT");
    response.setSggCd("26350");
    response.setUmdNm("우동");
    response.setJibun("100");
    response.setBuildYear(2020);
    response.setLatestSalePrice(10000L);
    response.setLatestDeposit(5000L);
    response.setLatestMonthlyRent(50L);
    return response;
  }

  private PropertyDealHistoryResponse emptyHistories() {
    return new PropertyDealHistoryResponse(
        List.of(),
        List.of(),
        1,
        3,
        0,
        0,
        "JEONSE",
        1,
        3,
        0,
        0,
        0,
        0
    );
  }

  private PropertyAiComparisonResponse comparisonResponse(String recommendedProperty) {
    PropertyAiComparisonResponse response = new PropertyAiComparisonResponse();
    response.setRecommendedProperty(recommendedProperty);
    response.setOneLineSummary(recommendedProperty + " 추천");
    response.setRecommendationReason(recommendedProperty + "가 더 적합합니다.");
    return response;
  }
}
