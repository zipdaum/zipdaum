package com.ssafy.zipdaum.recommendation.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Long latestSalePrice;
  private Long latestDeposit;
  private Long latestMonthlyRent;
  private Long latestMonthlyRentDeposit;
  private Long latestMonthlyRentAmount;
  private Long latestDealPrice;
  private LocalDate latestDealDate;
  private BigDecimal exclusiveArea;
  private Integer interactionViewCount;
  private Long interactionTotalDwellTimeMillis;
  private Integer interactionMaxScrollDepthPercent;
  private Integer recommendationDetailClickCount;
  private Integer dealHistoryClickCount;
}
