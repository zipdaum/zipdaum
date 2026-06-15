package com.ssafy.zipdaum.property.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SurroundingSummaryResponse {

  private int busCount;
  private int subwayCount;
  private int hospitalCount;
  private int cctvCount;
  private int parkCount;
}
