package com.ssafy.zipdaum.recommendation.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PropertyRecommendationCandidateFilter {

  private Long userId;
  private String region;
  private Long salePriceMax;
  private Long depositMax;
  private Long monthlyRentMax;
  private BigDecimal minExclusiveArea;
  private boolean monthlyRentPreferred;
  private boolean interactedOnly;
}
