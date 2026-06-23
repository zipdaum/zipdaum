package com.ssafy.zipdaum.property.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PropertyAiComparisonItem {

  private String criterion;
  private String propertyA;
  private String propertyB;
  private String better;
  private String reason;
}
