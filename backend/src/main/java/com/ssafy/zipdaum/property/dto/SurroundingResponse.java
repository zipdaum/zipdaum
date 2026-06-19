package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurroundingResponse {

  private BigDecimal latitude;
  private BigDecimal longitude;
  private int radiusMeters;
  private SurroundingSummaryResponse summary;
  private List<SurroundingFacilityResponse> facilities;
}
