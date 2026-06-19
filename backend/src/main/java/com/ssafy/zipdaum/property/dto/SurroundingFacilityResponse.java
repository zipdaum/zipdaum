package com.ssafy.zipdaum.property.dto;

import com.ssafy.zipdaum.property.domain.SurroundingType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurroundingFacilityResponse {

  private SurroundingType type;
  private String name;
  private String address;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Integer distanceMeters;
  private String source;
  private String detail;
}
