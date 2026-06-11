package com.ssafy.zipdaum.property.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertySearchRequest {

  private String sggCd;
  private String umdNm;
  private String name;
  private String propertyType;
  private String dealType;
  private Long minPrice;
  private Long maxPrice;
}
