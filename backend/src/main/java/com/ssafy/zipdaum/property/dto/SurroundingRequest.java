package com.ssafy.zipdaum.property.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SurroundingRequest {

  @Positive
  @Max(3000)
  private Integer radiusMeters;
}
