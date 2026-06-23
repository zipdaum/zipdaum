package com.ssafy.zipdaum.recommendation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.zipdaum.global.ai.GmsOpenAiClient;
import com.ssafy.zipdaum.global.util.RedisUtil;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.config.AiSummaryCacheProperties;
import com.ssafy.zipdaum.recommendation.dto.PropertyAiSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PropertyAiSummaryServiceImplTest {

  private final RecommendationMapper recommendationMapper = mock(RecommendationMapper.class);
  private final RecommendationService recommendationService = mock(RecommendationService.class);
  private final SurroundingService surroundingService = mock(SurroundingService.class);
  private final GmsOpenAiClient gmsOpenAiClient = mock(GmsOpenAiClient.class);
  private final RedisUtil redisUtil = mock(RedisUtil.class);
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final AiSummaryCacheProperties cacheProperties = cacheProperties();
  private final PropertyAiSummaryServiceImpl service = new PropertyAiSummaryServiceImpl(
      recommendationMapper,
      recommendationService,
      surroundingService,
      gmsOpenAiClient,
      redisUtil,
      objectMapper,
      cacheProperties
  );

  @Test
  void summarizeProperty_캐시가_있으면_AI를_호출하지_않고_반환한다() {
    PropertyRecommendationCandidate property = property();
    PropertyRecommendationScore score = new PropertyRecommendationScore(90, List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L))
        .willReturn(property);
    given(recommendationService.findPropertyRecommendationScore(1L, 10L))
        .willReturn(score);
    given(redisUtil.getData(startsWith("property-ai-summary:v1:1:10:")))
        .willReturn("{\"summary\":\"cached summary\"}");

    PropertyAiSummaryResponse result = service.summarizeProperty(1L, 10L);

    assertThat(result.getSummary()).isEqualTo("cached summary");
    then(gmsOpenAiClient).shouldHaveNoInteractions();
  }

  @Test
  void summarizeProperty_캐시가_없으면_AI_응답을_1시간_TTL로_저장한다() {
    PropertyRecommendationCandidate property = property();
    PropertyRecommendationScore score = new PropertyRecommendationScore(90, List.of());

    given(recommendationMapper.selectPropertyRecommendationCandidate(1L, 10L))
        .willReturn(property);
    given(recommendationService.findPropertyRecommendationScore(1L, 10L))
        .willReturn(score);
    given(redisUtil.getData(startsWith("property-ai-summary:v1:1:10:")))
        .willReturn(null);
    given(gmsOpenAiClient.chatCompletion(anyString(), anyString(), eq("property summary")))
        .willReturn("generated summary");

    PropertyAiSummaryResponse result = service.summarizeProperty(1L, 10L);

    assertThat(result.getSummary()).isEqualTo("generated summary");
    ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
    then(redisUtil).should().setDataWithTTL(
        startsWith("property-ai-summary:v1:1:10:"),
        valueCaptor.capture(),
        eq(3600L)
    );
    assertThat(valueCaptor.getValue()).contains("generated summary");
  }

  private PropertyRecommendationCandidate property() {
    PropertyRecommendationCandidate property = new PropertyRecommendationCandidate();
    property.setId(10L);
    property.setName("테스트 주택");
    property.setSggNm("부산광역시 해운대구");
    property.setUmdNm("우동");
    return property;
  }

  private AiSummaryCacheProperties cacheProperties() {
    AiSummaryCacheProperties properties = new AiSummaryCacheProperties();
    properties.setVersion("v1");
    properties.setTtl(Duration.ofHours(1));
    return properties;
  }
}
