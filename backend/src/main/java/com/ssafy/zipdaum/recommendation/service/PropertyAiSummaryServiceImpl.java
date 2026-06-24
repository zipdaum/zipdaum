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

  private static final String AI_SUMMARY_PROMPT_VERSION = "prompt-v2";
  private static final String DEVELOPER_PROMPT = """
      당신은 ZipDaum 주택 상세 화면의 AI 요약 도우미입니다.
      입력으로 제공된 주택 정보, 사용자 맞춤 적합도, 조건별 평가, 주변시설 요약만 근거로 사용합니다.
      입력에 없는 사실, 미래 가격 전망, 투자 가치, 확정적 안전성, 실제 계약 가능성은 추정하지 않습니다.
      판단 우선순위는 사용자 선호 조건 및 조건별 평가, 가격 조건, 최근 거래 정보, 입지/주변시설 순서입니다.
      null, 정보 없음, 빈 조건 목록, 빈 주변시설 요약, 0 값은 사용자에게 유리한 근거로 해석하지 않습니다.
      0 값이 실제 값인지 결측 대체값인지 불명확하면 유리하거나 불리한 핵심 근거로 사용하지 않습니다.
      점수와 시설 개수를 과장하지 말고 입력값 그대로 해석합니다.
      응답 전 내부적으로 다음을 점검합니다: summary 필드가 있는지, 입력 근거만 사용했는지, 한 문장인지, 사고 과정이 노출되지 않았는지.
      내부 점검 과정이나 단계별 사고 과정은 출력하지 않습니다.
      응답은 반드시 유효한 JSON 객체 하나로만 작성하고, 마크다운이나 코드블록을 포함하지 않습니다.
      JSON 스키마는 {"summary":"80자 이상 160자 이하의 한국어 한 문장"} 입니다.
      """;

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
        DEVELOPER_PROMPT,
        prompt,
        "property summary"
    );
    PropertyAiSummaryResponse response = parseSummaryResponse(summary);
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
          AI_SUMMARY_PROMPT_VERSION,
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

  private PropertyAiSummaryResponse parseSummaryResponse(String content) {
    try {
      AiSummaryOutput output = objectMapper.readValue(extractJsonObject(content), AiSummaryOutput.class);
      if (output.summary() == null || output.summary().isBlank()) {
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
      }
      return new PropertyAiSummaryResponse(output.summary().trim());
    } catch (JsonProcessingException e) {
      log.warn("주택 AI 요약 응답 파싱 실패", e);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private String extractJsonObject(String content) {
    if (content == null) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }

    String trimmed = content.trim();
    if (trimmed.startsWith("```")) {
      int firstLineEnd = trimmed.indexOf('\n');
      int lastFenceStart = trimmed.lastIndexOf("```");
      if (firstLineEnd >= 0 && lastFenceStart > firstLineEnd) {
        trimmed = trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
      }
    }

    int objectStart = trimmed.indexOf('{');
    int objectEnd = trimmed.lastIndexOf('}');
    if (objectStart < 0 || objectEnd < objectStart) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
    return trimmed.substring(objectStart, objectEnd + 1);
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
        아래 입력 데이터를 근거로 주택 상세 제목 하단에 표시할 AI 요약을 작성하세요.
        응답 형식은 developer 지시의 JSON 스키마를 따르세요.

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

  private record AiSummaryOutput(String summary) {
  }

  private record AiSummaryCacheInput(
      String version,
      String promptVersion,
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
