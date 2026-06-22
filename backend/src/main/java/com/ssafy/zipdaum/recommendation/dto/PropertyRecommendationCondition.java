package com.ssafy.zipdaum.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PropertyRecommendationCondition {

  private String code;
  private String name;
  private String value;
  private Integer priority;
  private boolean matched;
  private int score;
  private String reason;
}
