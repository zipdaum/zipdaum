package com.ssafy.zipdaum.property.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class RentDealDto {
  private Long id;
  private Long propertyId; // 외래키 매핑
  private BigDecimal exclusiveArea;
  private BigDecimal landArea;
  private Long deposit;
  private Long monthlyRent;
  private Integer floor;
  private String contractTerm;
  private String contractType;
  private Boolean useRrRight;
  private Long preDeposit;
  private Long preMonthlyRent;
  private LocalDate dealDate;
  private LocalDateTime createdAt;
}
