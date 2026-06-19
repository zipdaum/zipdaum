package com.ssafy.zipdaum.property.dto;

import com.ssafy.zipdaum.property.domain.SurroundingType;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FacilitySource {

  private final SurroundingType type;
  private final String name;
  private final String address;
  private final BigDecimal latitude;
  private final BigDecimal longitude;
  private final String source;
  private final String detail;
}
