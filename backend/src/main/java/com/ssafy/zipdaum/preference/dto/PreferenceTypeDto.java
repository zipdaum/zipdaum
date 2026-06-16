package com.ssafy.zipdaum.preference.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PreferenceTypeDto {
  private Long id;
  private String code;
  private String name;
  private String description;
}
