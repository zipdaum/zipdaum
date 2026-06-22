package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertySaveCommand {

  private Long id;
  private String propertyType;
  private String name;
  private String sggCd;
  private String umdNm;
  private String jibun;
  private Integer buildYear;
  private BigDecimal latitude;
  private BigDecimal longitude;
}
