package com.ssafy.zipdaum.recommendation.dto;

import java.util.List;
import lombok.Getter;

@Getter
public class PropertyRecommendationScore {

  private final RecommendationStatus recommendationStatus;
  private final Integer score;
  private final List<PropertyRecommendationCondition> conditions;

  public PropertyRecommendationScore(
      Integer score,
      List<PropertyRecommendationCondition> conditions) {
    this(RecommendationStatus.EVALUATED, score, conditions);
  }

  public PropertyRecommendationScore(
      RecommendationStatus recommendationStatus,
      Integer score,
      List<PropertyRecommendationCondition> conditions) {
    this.recommendationStatus = recommendationStatus;
    this.score = score;
    this.conditions = conditions;
  }
}
