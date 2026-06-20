package com.ssafy.zipdaum.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPropertyInteractionSaveCommand {

  private Long userId;
  private Long propertyId;
  private Long dwellTimeMillis;
  private Integer maxScrollDepthPercent;
  private Boolean recommendationDetailClicked;
  private Boolean dealHistoryClicked;
}
