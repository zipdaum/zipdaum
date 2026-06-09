package com.ssafy.zipdaum.property.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class SaleDealDto {
  private Long id;
  private Long propertyId; // 외래키 매핑
  private BigDecimal exclusiveArea;
  private BigDecimal landArea;
  private Long dealAmount;
  private Integer floor;
  private LocalDateTime dealDate;
  private String buyerGbn;
  private String sellerGbn;
  private LocalDateTime createdAt;
}
