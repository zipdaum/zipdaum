package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;
import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationResponse;
import java.util.List;

public interface RecommendationService {

  PropertyRecommendationScore findPropertyRecommendationScore(Long userId, Long propertyId);

  List<PropertyRecommendationResponse> findPropertyRecommendations(Long userId);
}
