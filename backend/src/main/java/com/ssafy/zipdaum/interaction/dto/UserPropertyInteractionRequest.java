package com.ssafy.zipdaum.interaction.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPropertyInteractionRequest {

  @NotNull
  @Min(0)
  private Long dwellTimeMillis;

  @NotNull
  @Min(0)
  @Max(100)
  private Integer maxScrollDepthPercent;

  @NotNull
  private Boolean recommendationDetailClicked;

  @NotNull
  private Boolean dealHistoryClicked;
}
