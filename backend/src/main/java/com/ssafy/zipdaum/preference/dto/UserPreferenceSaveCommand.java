package com.ssafy.zipdaum.preference.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserPreferenceSaveCommand {
  private Long preferenceTypeId;
  private String preferenceValue;
  private Integer priority;
}
