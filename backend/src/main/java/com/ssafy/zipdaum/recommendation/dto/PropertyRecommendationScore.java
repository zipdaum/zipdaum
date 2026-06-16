package com.ssafy.zipdaum.recommendation.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PropertyRecommendationScore {

  private int score;
  private int evaluatedCount;
  private int matchedCount;
  private List<String> matchedReasons;
  private List<PropertyRecommendationCondition> conditions;
}
