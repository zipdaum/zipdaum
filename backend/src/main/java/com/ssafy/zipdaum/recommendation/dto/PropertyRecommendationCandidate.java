package com.ssafy.zipdaum.recommendation.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyRecommendationCandidate {

  private Long id;
  private String propertyType;
  private String name;
  private String sggCd;
  private String umdNm;
  private String jibun;
  private Integer buildYear;
  private Long latestSalePrice;
  private Long latestDeposit;
  private Long latestMonthlyRent;
  private BigDecimal exclusiveArea;
}
