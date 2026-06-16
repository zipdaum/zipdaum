package com.ssafy.zipdaum.preference.domain;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum UserPreferenceType {
  BUDGET("예산"),
  AREA("면적"),
  BUILD_YEAR("건축연도"),
  REGION("지역"),
  BUS("버스"),
  SUBWAY("지하철역"),
  HOSPITAL("병원"),
  CCTV("방범용 CCTV"),
  PARK("공원");

  private final String displayName;

  UserPreferenceType(String displayName) {
    this.displayName = displayName;
  }

  public static UserPreferenceType fromCode(String code) {
    return Arrays.stream(values())
        .filter(type -> type.name().equals(code))
        .findFirst()
        .orElseThrow(IllegalArgumentException::new);
  }
}
