package com.ssafy.zipdaum.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.zipdaum.global.ai.GmsOpenAiClient;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.global.util.RedisUtil;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.config.AiSummaryCacheProperties;
import com.ssafy.zipdaum.recommendation.dto.PropertyAiSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCondition;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyAiSummaryServiceImpl implements PropertyAiSummaryService {

  private final RecommendationMapper recommendationMapper;
  private final RecommendationService recommendationService;
  private final SurroundingService surroundingService;
  private final GmsOpenAiClient gmsOpenAiClient;
  private final RedisUtil redisUtil;
  private final ObjectMapper objectMapper;
  private final AiSummaryCacheProperties cacheProperties;

  @Override
  @Transactional(readOnly = true)
  public PropertyAiSummaryResponse summarizeProperty(Long userId, Long propertyId) {
    PropertyRecommendationCandidate property =
        recommendationMapper.selectPropertyRecommendationCandidate(userId, propertyId);

    if (property == null) {
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    PropertyRecommendationScore score =
        recommendationService.findPropertyRecommendationScore(userId, propertyId);
    SurroundingSummaryResponse surroundingSummary = findSurroundingSummary(property);
    String prompt = createPrompt(property, score, surroundingSummary);
    String cacheKey = null;
    String inputHash = createInputHash(property, score, surroundingSummary);
    if (inputHash != null) {
      cacheKey = buildCacheKey(userId, propertyId, inputHash);
      PropertyAiSummaryResponse cachedResponse = findCachedResponse(cacheKey);
      if (cachedResponse != null) {
        return cachedResponse;
      }
    }

    String summary = gmsOpenAiClient.chatCompletion(
        "Answer in Korean. Keep the answer to one natural sentence.",
        prompt,
        "property summary"
    );
    PropertyAiSummaryResponse response = new PropertyAiSummaryResponse(summary);
    if (cacheKey != null) {
      cacheResponse(cacheKey, response);
    }
    return response;
  }

  private String buildCacheKey(Long userId, Long propertyId, String inputHash) {
    return "property-ai-summary:%s:%d:%d:%s"
        .formatted(cacheProperties.getVersion(), userId, propertyId, inputHash);
  }

  private String createInputHash(
      PropertyRecommendationCandidate property,
      PropertyRecommendationScore score,
      SurroundingSummaryResponse surroundingSummary) {
    try {
      AiSummaryCacheInput input = new AiSummaryCacheInput(
          cacheProperties.getVersion(),
          PropertyCacheInput.from(property),
          ScoreCacheInput.from(score),
          SurroundingCacheInput.from(surroundingSummary)
      );
      String json = objectMapper.writeValueAsString(input);
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (JsonProcessingException | NoSuchAlgorithmException e) {
      log.warn("주택 AI 요약 캐시 해시 생성 실패 propertyId={}", property.getId(), e);
      return null;
    }
  }

  private PropertyAiSummaryResponse findCachedResponse(String cacheKey) {
    try {
      String cached = redisUtil.getData(cacheKey);
      if (cached == null || cached.isBlank()) {
        return null;
      }
      return objectMapper.readValue(cached, PropertyAiSummaryResponse.class);
    } catch (JsonProcessingException e) {
      log.warn("주택 AI 요약 캐시 응답 파싱 실패", e);
      deleteCache(cacheKey);
      return null;
    } catch (RuntimeException e) {
      log.warn("주택 AI 요약 캐시 조회 실패", e);
      return null;
    }
  }

  private void cacheResponse(String cacheKey, PropertyAiSummaryResponse response) {
    try {
      redisUtil.setDataWithTTL(
          cacheKey,
          objectMapper.writeValueAsString(response),
          cacheProperties.getTtl().toSeconds()
      );
    } catch (JsonProcessingException e) {
      log.warn("주택 AI 요약 캐시 응답 직렬화 실패", e);
    } catch (RuntimeException e) {
      log.warn("주택 AI 요약 캐시 저장 실패", e);
    }
  }

  private void deleteCache(String cacheKey) {
    try {
      redisUtil.delete(cacheKey);
    } catch (RuntimeException e) {
      log.warn("주택 AI 요약 캐시 삭제 실패", e);
    }
  }

  private String createPrompt(
      PropertyRecommendationCandidate property,
      PropertyRecommendationScore score,
      SurroundingSummaryResponse surroundingSummary) {
    return """
        다음 주택이 현재 로그인 사용자의 조건에 얼마나 잘 맞는지 제목 아래에 붙일 한 문장으로 요약해 주세요.
        조건:
        - 80자에서 160자 사이의 자연스러운 한국어 문장
        - 사용자가 이해하기 쉬운 말투
        - 숫자와 근거가 있으면 반영
        - 과장하지 말고 제공된 데이터만 사용
        - JSON이나 마크다운 없이 문장만 출력

        주택 정보:
        - 이름: %s
        - 유형: %s
        - 위치: %s %s %s
        - 준공연도: %s
        - 전용면적: %s㎡
        - 최근 매매가: %s원
        - 최근 전세 보증금: %s원
        - 최근 월세: 보증금 %s원 / 월세 %s원
        - 최근 거래금액: %s원
        - 최근 거래일: %s
        - 주변시설: %s
        - 추천 상태: %s
        - 추천 점수: %s

        조건별 평가:
        %s
        """.formatted(
        value(property.getName()),
        value(property.getPropertyType()),
        value(property.getSggNm()),
        value(property.getUmdNm()),
        value(property.getJibun()),
        value(property.getBuildYear()),
        value(property.getExclusiveArea()),
        value(property.getLatestSalePrice()),
        value(property.getLatestDeposit()),
        value(property.getLatestMonthlyRentDeposit()),
        value(property.getLatestMonthlyRentAmount()),
        value(property.getLatestDealPrice()),
        value(property.getLatestDealDate()),
        surroundingLine(surroundingSummary),
        score.getRecommendationStatus(),
        value(score.getScore()),
        conditionLines(score.getConditions())
    );
  }

  private SurroundingSummaryResponse findSurroundingSummary(PropertyRecommendationCandidate property) {
    if (property.getLatitude() == null || property.getLongitude() == null) {
      return null;
    }
    return surroundingService.findRecommendationSurroundingSummary(
        property.getLatitude(),
        property.getLongitude()
    );
  }

  private String surroundingLine(SurroundingSummaryResponse summary) {
    if (summary == null) {
      return "좌표 정보 없음";
    }
    return "버스정류장 500m 내 %d개, 지하철역 1km 내 %d개, 병원 1.5km 내 %d개, CCTV 500m 내 %d개, 공원 1km 내 %d개"
        .formatted(
            summary.getBusCount(),
            summary.getSubwayCount(),
            summary.getHospitalCount(),
            summary.getCctvCount(),
            summary.getParkCount()
        );
  }

  private String conditionLines(List<PropertyRecommendationCondition> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return "- 평가 가능한 조건이 없습니다.";
    }

    return String.join(System.lineSeparator(), conditions.stream()
        .map(condition -> "- %s: %s, 점수 %d, 사유: %s".formatted(
            condition.getName(),
            condition.isMatched() ? "적합" : "부분 적합 또는 부적합",
            condition.getScore(),
            value(condition.getReason())
        ))
        .toList());
  }

  private String value(String value) {
    return value == null || value.isBlank() ? "정보 없음" : value;
  }

  private String value(Long value) {
    return value == null ? "정보 없음" : value.toString();
  }

  private String value(Integer value) {
    return value == null ? "정보 없음" : value.toString();
  }

  private String value(BigDecimal value) {
    return value == null ? "정보 없음" : value.stripTrailingZeros().toPlainString();
  }

  private String value(LocalDate value) {
    return value == null ? "정보 없음" : value.toString();
  }

  private record AiSummaryCacheInput(
      String version,
      PropertyCacheInput property,
      ScoreCacheInput score,
      SurroundingCacheInput surroundingSummary) {
  }

  private record PropertyCacheInput(
      String name,
      String propertyType,
      String sggNm,
      String umdNm,
      String jibun,
      Integer buildYear,
      BigDecimal exclusiveArea,
      Long latestSalePrice,
      Long latestDeposit,
      Long latestMonthlyRentDeposit,
      Long latestMonthlyRentAmount,
      Long latestDealPrice,
      LocalDate latestDealDate) {

    private static PropertyCacheInput from(PropertyRecommendationCandidate property) {
      return new PropertyCacheInput(
          property.getName(),
          property.getPropertyType(),
          property.getSggNm(),
          property.getUmdNm(),
          property.getJibun(),
          property.getBuildYear(),
          property.getExclusiveArea(),
          property.getLatestSalePrice(),
          property.getLatestDeposit(),
          property.getLatestMonthlyRentDeposit(),
          property.getLatestMonthlyRentAmount(),
          property.getLatestDealPrice(),
          property.getLatestDealDate()
      );
    }
  }

  private record ScoreCacheInput(
      Object recommendationStatus,
      Integer score,
      List<ConditionCacheInput> conditions) {

    private static ScoreCacheInput from(PropertyRecommendationScore score) {
      return new ScoreCacheInput(
          score.getRecommendationStatus(),
          score.getScore(),
          sortedConditions(score.getConditions())
      );
    }

    private static List<ConditionCacheInput> sortedConditions(
        List<PropertyRecommendationCondition> conditions) {
      if (conditions == null || conditions.isEmpty()) {
        return List.of();
      }
      return conditions.stream()
          .map(ConditionCacheInput::from)
          .sorted(Comparator
              .comparing(ConditionCacheInput::priority, Comparator.nullsLast(Integer::compareTo))
              .thenComparing(ConditionCacheInput::code, Comparator.nullsLast(String::compareTo))
              .thenComparing(ConditionCacheInput::name, Comparator.nullsLast(String::compareTo))
              .thenComparing(ConditionCacheInput::value, Comparator.nullsLast(String::compareTo))
              .thenComparing(ConditionCacheInput::reason, Comparator.nullsLast(String::compareTo)))
          .toList();
    }
  }

  private record ConditionCacheInput(
      String code,
      String name,
      String value,
      Integer priority,
      boolean matched,
      int score,
      String reason) {

    private static ConditionCacheInput from(PropertyRecommendationCondition condition) {
      return new ConditionCacheInput(
          condition.getCode(),
          condition.getName(),
          condition.getValue(),
          condition.getPriority(),
          condition.isMatched(),
          condition.getScore(),
          condition.getReason()
      );
    }
  }

  private record SurroundingCacheInput(
      Integer busCount,
      Integer subwayCount,
      Integer hospitalCount,
      Integer cctvCount,
      Integer parkCount) {

    private static SurroundingCacheInput from(SurroundingSummaryResponse summary) {
      if (Objects.isNull(summary)) {
        return null;
      }
      return new SurroundingCacheInput(
          summary.getBusCount(),
          summary.getSubwayCount(),
          summary.getHospitalCount(),
          summary.getCctvCount(),
          summary.getParkCount()
      );
    }
  }
}
