package com.ssafy.zipdaum.preference.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserPreferenceDto extends BaseDto {
  private Long id;
  private Long userId;          // 외래키 매핑
  private Long preferenceTypeId; // 외래키 매핑
  private String preferenceValue;
  private Integer priority;
}
