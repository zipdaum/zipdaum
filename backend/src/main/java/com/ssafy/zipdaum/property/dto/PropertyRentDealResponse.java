package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyRentDealResponse {

  private Long id;
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
}
