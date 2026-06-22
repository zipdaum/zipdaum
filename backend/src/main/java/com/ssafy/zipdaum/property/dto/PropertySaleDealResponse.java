package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertySaleDealResponse {

  private Long id;
  private BigDecimal exclusiveArea;
  private BigDecimal landArea;
  private Long dealAmount;
  private Integer floor;
  private LocalDate dealDate;
  private String buyerGbn;
  private String sellerGbn;
}
