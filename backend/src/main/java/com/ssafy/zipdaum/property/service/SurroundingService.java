package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.dto.SurroundingResponse;
import java.math.BigDecimal;

public interface SurroundingService {

  SurroundingResponse findPropertySurroundings(Long propertyId, Integer radiusMeters);

  SurroundingResponse findSurroundings(BigDecimal latitude, BigDecimal longitude, Integer radiusMeters);
}
