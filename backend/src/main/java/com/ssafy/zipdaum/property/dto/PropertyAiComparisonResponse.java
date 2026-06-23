package com.ssafy.zipdaum.property.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PropertyAiComparisonResponse {

  private String oneLineSummary;
  private String recommendedProperty;
  private String recommendationReason;
  private List<PropertyAiComparisonItem> comparisonTable = new ArrayList<>();
  private List<String> propertyAPros = new ArrayList<>();
  private List<String> propertyACons = new ArrayList<>();
  private List<String> propertyBPros = new ArrayList<>();
  private List<String> propertyBCons = new ArrayList<>();
  private List<String> cautions = new ArrayList<>();
  private PropertyAiRecommendedFor recommendedFor;
}
