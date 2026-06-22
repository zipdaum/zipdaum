package com.ssafy.zipdaum.property.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyAiComparisonRequest {

  @Size(min = 2, max = 2)
  private List<@Positive Long> propertyIds;

  @Pattern(regexp = "SALE|JEONSE|MONTHLY_RENT|UNKNOWN")
  private String selectedDealType = "UNKNOWN";

  private String comparisonPurpose = "실거주 관점의 주택 비교";
}
