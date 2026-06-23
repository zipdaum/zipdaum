package com.ssafy.zipdaum.recommendation.service;

import com.ssafy.zipdaum.recommendation.dto.PropertyAiSummaryResponse;

public interface PropertyAiSummaryService {

  PropertyAiSummaryResponse summarizeProperty(Long userId, Long propertyId);
}
