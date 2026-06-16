package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.property.dto.SurroundingResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.service.SurroundingService;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.mapper.RecommendationMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

  private static final int RECOMMENDATION_SURROUNDING_RADIUS_METERS = 1000;

  private final RecommendationMapper recommendationMapper;
  private final UserPreferenceService userPreferenceService;
  private final SurroundingService surroundingService;
  private final RecommendationScoreService recommendationScoreService;

  @Override
  @Transactional(readOnly = true)
  public PropertyRecommendationScore findPropertyRecommendationScore(Long userId, Long propertyId) {
    PropertyRecommendationCandidate property =
        recommendationMapper.selectPropertyRecommendationCandidate(propertyId);

    if (property == null) {
      log.warn("주택 맞춤 조건 적합도 조회 실패 - 존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    List<UserPreferenceResponse> preferences = userPreferenceService.findPreferences(userId);
    SurroundingSummaryResponse surroundingSummary = findSurroundingSummary(propertyId, preferences);

    PropertyRecommendationScore score = recommendationScoreService.calculateMatchScore(
        property,
        preferences,
        surroundingSummary
    );

    log.info("주택 맞춤 조건 적합도 계산 완료 userId={}, propertyId={}, score={}",
        userId, propertyId, score.getScore());
    return score;
  }

  private SurroundingSummaryResponse findSurroundingSummary(
      Long propertyId,
      List<UserPreferenceResponse> preferences) {
    if (!hasFacilityPreference(preferences)) {
      return null;
    }

    try {
      SurroundingResponse response = surroundingService.findPropertySurroundings(
          propertyId,
          RECOMMENDATION_SURROUNDING_RADIUS_METERS
      );
      return response.getSummary();
    } catch (BusinessException e) {
      if (e.getErrorCode() == ErrorCode.COORDINATE_NOT_FOUND) {
        log.debug("주택 좌표 정보 없음 - 주변시설 조건은 불일치 처리 propertyId={}", propertyId);
        return null;
      }
      throw e;
    }
  }

  private boolean hasFacilityPreference(List<UserPreferenceResponse> preferences) {
    return preferences.stream()
        .anyMatch(preference ->
            isFacilityCode(preference.getCode()) && "true".equalsIgnoreCase(preference.getValue())
        );
  }

  private boolean isFacilityCode(String code) {
    if (code == null) {
      return false;
    }
    String normalizedCode = code.trim().toUpperCase();
    return "BUS".equals(normalizedCode)
        || "SUBWAY".equals(normalizedCode)
        || "HOSPITAL".equals(normalizedCode)
        || "CCTV".equals(normalizedCode)
        || "PARK".equals(normalizedCode);
  }
}
