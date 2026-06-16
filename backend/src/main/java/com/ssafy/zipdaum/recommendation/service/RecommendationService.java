package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.recommendation.dto.PropertyRecommendationScore;

public interface RecommendationService {

  PropertyRecommendationScore findPropertyRecommendationScore(Long userId, Long propertyId);
}
