package com.ssafy.zipdaum.property.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.zipdaum.favorite.dto.FavoritePropertyResponse;
import com.ssafy.zipdaum.favorite.service.FavoritePropertyService;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.config.PropertyAiProperties;
import com.ssafy.zipdaum.property.dto.PropertyAiComparisonRequest;
import com.ssafy.zipdaum.property.dto.PropertyAiComparisonResponse;
import com.ssafy.zipdaum.property.dto.PropertyDealHistoryResponse;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.service.RecommendationService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
@Slf4j
public class PropertyAiComparisonServiceImpl implements PropertyAiComparisonService {

  private static final int SALE_HISTORY_PAGE = 1;
  private static final int RENT_HISTORY_PAGE = 1;
  private static final int HISTORY_SIZE = 3;
  private static final int SURROUNDING_RADIUS_METERS = 1000;
  private static final String DEFAULT_RENT_DEAL_TYPE = "JEONSE";
  private static final String DEVELOPER_PROMPT = """
      너는 ZipDaum의 주택 비교 도우미다. 반드시 한국어로 답한다.
      입력으로 제공된 데이터만 근거로 두 주택을 비교한다.
      없는 정보는 추측하지 말고 '제공된 정보만으로는 판단하기 어렵습니다'라고 말한다.
      null, 빈 배열, 0 값은 유리한 근거로 사용하지 않는다. 0 값이 실제 가격인지 데이터 없음인지 불명확하면 판단 보류로 설명한다.
      추천 점수, 거래 금액, 주변시설 개수는 재계산하지 말고 입력값을 그대로 해석한다.
      판단 우선순위는 사용자 선호 조건과 조건별 적합도 reason, 거래 가격과 보증금/월세 조건, 최근 거래 이력, 입지와 주변시설, 관심 주택 여부 순서다.
      투자 수익률, 미래 가격 상승, 확정적 가치 판단은 하지 않는다.
      예시는 'A 주택: 예산 적합, 병원 가까움, 최근 거래가 안정적 / B 주택: 가격은 좋지만 선호 시설 부족'처럼 짧고 구체적인 근거로 설명한다.
      응답 전에 내부적으로 단계별로 근거를 점검하되, 사고 과정은 출력하지 않는다.
      recommendedProperty는 반드시 'A', 'B', 'NONE' 중 하나로 작성한다. 우열이 명확하지 않으면 'NONE'을 사용한다.
      oneLineSummary는 '실거주 관점에서는 ... 때문에 A/B가 더 적합합니다.' 또는 우열이 명확하지 않을 때 '제공된 정보만으로는 A와 B의 우열을 판단하기 어렵습니다.' 형식으로 작성한다.
      응답은 반드시 유효한 JSON 객체로만 작성한다.
      """;

  private final PropertyAiProperties properties;
  private final PropertyService propertyService;
  private final SurroundingService surroundingService;
  private final RecommendationService recommendationService;
  private final UserPreferenceService userPreferenceService;
  private final FavoritePropertyService favoritePropertyService;
  private final RestClient restClient;
  private final ObjectMapper objectMapper;

  public PropertyAiComparisonServiceImpl(
      PropertyAiProperties properties,
      PropertyService propertyService,
      SurroundingService surroundingService,
      RecommendationService recommendationService,
      UserPreferenceService userPreferenceService,
      FavoritePropertyService favoritePropertyService,
      @Qualifier("propertyAiRestClient") RestClient restClient,
      ObjectMapper objectMapper) {
    this.properties = properties;
    this.propertyService = propertyService;
    this.surroundingService = surroundingService;
    this.recommendationService = recommendationService;
    this.userPreferenceService = userPreferenceService;
    this.favoritePropertyService = favoritePropertyService;
    this.restClient = restClient;
    this.objectMapper = objectMapper;
  }

  @Override
  public PropertyAiComparisonResponse compareProperties(
      Long userId,
      PropertyAiComparisonRequest request) {
    validateRequest(request);
    validateApiKey();

    List<Long> propertyIds = request.getPropertyIds();
    UserComparisonContext userContext = buildUserContext(userId, request);
    PropertyComparisonInput input = new PropertyComparisonInput(
        "두 주택 비교",
        outputFormat(),
        summarizeUserContext(userContext),
        List.of(
            buildPropertyInput("A", userId, propertyIds.get(0), userContext),
            buildPropertyInput("B", userId, propertyIds.get(1), userContext)
        ),
        comparisonRules()
    );

    try {
      String content = requestAiComparison(input);
      PropertyAiComparisonResponse response =
          objectMapper.readValue(content, PropertyAiComparisonResponse.class);
      log.info("AI 주택 비교 생성 완료 propertyA={}, propertyB={}",
          propertyIds.get(0), propertyIds.get(1));
      return response;
    } catch (JsonProcessingException e) {
      log.warn("AI 주택 비교 응답 파싱 실패 propertyA={}, propertyB={}",
          propertyIds.get(0), propertyIds.get(1), e);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private void validateRequest(PropertyAiComparisonRequest request) {
    if (request.getPropertyIds() == null || request.getPropertyIds().size() != 2) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    Long left = request.getPropertyIds().get(0);
    Long right = request.getPropertyIds().get(1);
    if (left == null || right == null || left < 1 || right < 1 || left.equals(right)) {
      throw new BusinessException(ErrorCode.INVALID_PROPERTY_ID);
    }
  }

  private void validateApiKey() {
    if (!properties.hasApiKey()) {
      log.warn("AI 주택 비교 실패 - GMS API 키 누락");
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private UserComparisonContext buildUserContext(Long userId, PropertyAiComparisonRequest request) {
    List<UserPreferenceResponse> preferences = findPreferences(userId);
    List<FavoritePropertyResponse> favoriteProperties = favoritePropertyService.findFavoriteProperties(userId);

    return new UserComparisonContext(
        normalizeComparisonPurpose(request.getComparisonPurpose()),
        preferences,
        favoriteProperties.stream()
            .map(FavoritePropertyResponse::getPropertyId)
            .collect(Collectors.toSet())
    );
  }

  private List<UserPreferenceResponse> findPreferences(Long userId) {
    try {
      return userPreferenceService.findPreferences(userId);
    } catch (BusinessException e) {
      if (e.getErrorCode() == ErrorCode.PREFERENCE_NOT_FOUND) {
        return List.of();
      }
      throw e;
    }
  }

  private PropertyComparisonTargetSummary buildPropertyInput(
      String label,
      Long userId,
      Long propertyId,
      UserComparisonContext userContext) {
    PropertyDetailResponse detail = propertyService.findPropertyDetail(propertyId);
    PropertyDealHistoryResponse histories = propertyService.findPropertyDealHistories(
        propertyId,
        DEFAULT_RENT_DEAL_TYPE,
        SALE_HISTORY_PAGE,
        RENT_HISTORY_PAGE,
        HISTORY_SIZE
    );

    return new PropertyComparisonTargetSummary(
        label,
        summarizeProperty(detail),
        summarizeDealHistories(histories),
        findSurroundingSummary(detail),
        summarizeRecommendationScore(findRecommendationScore(userId, propertyId)),
        userContext.favoritePropertyIds().contains(propertyId)
    );
  }

  private SurroundingSummaryResponse findSurroundingSummary(PropertyDetailResponse detail) {
    if (detail.getLatitude() == null || detail.getLongitude() == null) {
      return null;
    }
    return surroundingService.findSurroundingSummary(
        detail.getLatitude(),
        detail.getLongitude(),
        SURROUNDING_RADIUS_METERS
    );
  }

  private PropertyRecommendationScore findRecommendationScore(Long userId, Long propertyId) {
    try {
      return recommendationService.findPropertyRecommendationScore(userId, propertyId);
    } catch (BusinessException e) {
      if (e.getErrorCode() == ErrorCode.PROPERTY_NOT_FOUND) {
        throw e;
      }
      log.debug("AI 주택 비교 맞춤 점수 제외 userId={}, propertyId={}, errorCode={}",
          userId, propertyId, e.getErrorCode().name());
      return null;
    }
  }

  private String requestAiComparison(PropertyComparisonInput input) throws JsonProcessingException {
    ChatCompletionRequest request = new ChatCompletionRequest(
        properties.getModel(),
        List.of(
            new ChatMessage("developer", DEVELOPER_PROMPT),
            new ChatMessage("user", objectMapper.writeValueAsString(input))
        )
    );

    try {
      byte[] rawResponse = restClient.post()
          .uri(properties.getGmsUrl())
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
          .body(request)
          .retrieve()
          .body(byte[].class);

      JsonNode response = objectMapper.readTree(new String(rawResponse, StandardCharsets.UTF_8));
      String content = response == null
          ? null
          : response.path("choices").path(0).path("message").path("content").asText(null);
      if (content == null || content.isBlank()) {
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
      }
      return content;
    } catch (RestClientException e) {
      log.warn("AI 주택 비교 외부 API 호출 실패", e);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private UserComparisonContextSummary summarizeUserContext(UserComparisonContext userContext) {
    return new UserComparisonContextSummary(
        userContext.comparisonPurpose(),
        userContext.preferences(),
        userContext.favoritePropertyIds()
    );
  }

  private PropertySummary summarizeProperty(PropertyDetailResponse detail) {
    return new PropertySummary(
        detail.getId(),
        detail.getPropertyType(),
        detail.getName(),
        detail.getSggCd(),
        detail.getUmdNm(),
        detail.getJibun(),
        detail.getBuildYear(),
        detail.getLatestSalePrice(),
        detail.getLatestDeposit(),
        detail.getLatestMonthlyRent()
    );
  }

  private DealHistorySummary summarizeDealHistories(PropertyDealHistoryResponse histories) {
    return new DealHistorySummary(
        histories.getSaleDeals().stream()
            .map(deal -> new SaleDealSummary(
                deal.getDealAmount(),
                deal.getExclusiveArea(),
                deal.getFloor(),
                deal.getDealDate()
            ))
            .toList(),
        histories.getRentDeals().stream()
            .map(deal -> new RentDealSummary(
                deal.getDeposit(),
                deal.getMonthlyRent(),
                deal.getExclusiveArea(),
                deal.getFloor(),
                deal.getDealDate()
            ))
            .toList(),
        histories.getSaleTotalCount(),
        histories.getRentDealType(),
        histories.getRentTotalCount()
    );
  }

  private RecommendationScoreSummary summarizeRecommendationScore(PropertyRecommendationScore score) {
    if (score == null) {
      return null;
    }

    return new RecommendationScoreSummary(
        score.getRecommendationStatus(),
        score.getScore(),
        score.getConditions().stream()
            .map(condition -> new RecommendationConditionSummary(
                condition.getName(),
                condition.getValue(),
                condition.isMatched(),
                condition.getScore(),
                condition.getReason()
            ))
            .toList()
    );
  }

  private String normalizeComparisonPurpose(String comparisonPurpose) {
    if (comparisonPurpose == null || comparisonPurpose.isBlank()) {
      return "실거주 관점의 주택 비교";
    }
    return comparisonPurpose.trim();
  }

  private OutputFormat outputFormat() {
    return new OutputFormat(
        "string",
        "A | B | NONE",
        "string",
        List.of(new OutputComparisonItem(
            "가격 | 거래 이력 | 면적 | 연식 | 입지 | 주변시설 | 맞춤 적합도 | 사용자 관심",
            "string",
            "string",
            "A | B | NONE",
            "string"
        )),
        List.of("string"),
        List.of("string"),
        List.of("string"),
        List.of("string"),
        List.of("string"),
        new OutputRecommendedFor("string", "string")
    );
  }

  private List<String> comparisonRules() {
    return List.of(
        "사용자 선호 조건과 조건별 적합도 reason을 가장 우선적인 추천 근거로 삼는다.",
        "입력값에 없는 정보는 비교 근거에 포함하지 않는다.",
        "거래 이력이 비어 있거나 좌표가 없으면 해당 항목은 판단 보류로 설명한다.",
        "null, 빈 배열, 0 값은 유리한 근거로 해석하지 않는다.",
        "최종 추천은 A, B, NONE 중 하나로 정하고, 단정 대신 제공된 근거의 범위 안에서 안내한다.",
        "JSON을 반환하기 전에 recommendedProperty 값과 필수 필드 누락 여부를 내부적으로 검증한다."
    );
  }

  private record ChatCompletionRequest(String model, List<ChatMessage> messages) {
  }

  private record ChatMessage(String role, String content) {
  }

  private record PropertyComparisonInput(
      String task,
      OutputFormat outputFormat,
      UserComparisonContextSummary userContext,
      List<PropertyComparisonTargetSummary> properties,
      List<String> rules) {
  }

  private record UserComparisonContext(
      String comparisonPurpose,
      List<UserPreferenceResponse> preferences,
      Set<Long> favoritePropertyIds) {
  }

  private record UserComparisonContextSummary(
      String comparisonPurpose,
      List<UserPreferenceResponse> preferences,
      Set<Long> favoritePropertyIds) {
  }

  private record PropertyComparisonTargetSummary(
      String label,
      PropertySummary detail,
      DealHistorySummary recentDeals,
      SurroundingSummaryResponse surroundings,
      RecommendationScoreSummary recommendationScore,
      boolean favorite) {
  }

  private record PropertySummary(
      Long id,
      String propertyType,
      String name,
      String sggCd,
      String umdNm,
      String jibun,
      Integer buildYear,
      Long latestSalePrice,
      Long latestDeposit,
      Long latestMonthlyRent) {
  }

  private record DealHistorySummary(
      List<SaleDealSummary> saleDeals,
      List<RentDealSummary> rentDeals,
      long saleTotalCount,
      String rentDealType,
      long rentTotalCount) {
  }

  private record SaleDealSummary(
      Long dealAmount,
      Object exclusiveArea,
      Integer floor,
      Object dealDate) {
  }

  private record RentDealSummary(
      Long deposit,
      Long monthlyRent,
      Object exclusiveArea,
      Integer floor,
      Object dealDate) {
  }

  private record RecommendationScoreSummary(
      Object recommendationStatus,
      Integer score,
      List<RecommendationConditionSummary> conditions) {
  }

  private record RecommendationConditionSummary(
      String name,
      String value,
      boolean matched,
      int score,
      String reason) {
  }

  private record OutputFormat(
      String oneLineSummary,
      String recommendedProperty,
      String recommendationReason,
      List<OutputComparisonItem> comparisonTable,
      List<String> propertyAPros,
      List<String> propertyACons,
      List<String> propertyBPros,
      List<String> propertyBCons,
      List<String> cautions,
      OutputRecommendedFor recommendedFor) {
  }

  private record OutputComparisonItem(
      String criterion,
      String propertyA,
      String propertyB,
      String better,
      String reason) {
  }

  private record OutputRecommendedFor(String propertyA, String propertyB) {
  }
}
