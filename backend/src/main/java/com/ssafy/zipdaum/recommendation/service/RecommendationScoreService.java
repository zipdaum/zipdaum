package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationCandidate;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import java.util.List;

public interface RecommendationScoreService {

  PropertyRecommendationScore calculateMatchScore(
      PropertyRecommendationCandidate property,
      List<UserPreferenceResponse> preferences,
      SurroundingSummaryResponse surroundingSummary);
}
