package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.api.GmsOpenAiClient;
import com.ssafy.zipdaum.recommendation.dto.PropertyAiSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCondition;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyAiSummaryServiceImpl implements PropertyAiSummaryService {

  private final RecommendationMapper recommendationMapper;
  private final RecommendationService recommendationService;
  private final SurroundingService surroundingService;
  private final GmsOpenAiClient gmsOpenAiClient;

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
    String summary = gmsOpenAiClient.summarize(createPrompt(property, score, surroundingSummary));
    return new PropertyAiSummaryResponse(summary);
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
}
