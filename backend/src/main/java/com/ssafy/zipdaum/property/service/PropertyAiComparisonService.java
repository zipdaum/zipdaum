package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.dto.PropertyAiComparisonRequest;
import com.ssafy.zipdaum.property.dto.PropertyAiComparisonResponse;

public interface PropertyAiComparisonService {

  PropertyAiComparisonResponse compareProperties(
      Long userId,
      PropertyAiComparisonRequest request);
}
