package com.ssafy.zipdaum.condition.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConditionItemDto extends BaseDto {
  private Long id;
  private Long userId;          // 외래키 매핑
  private Long conditionTypeId; // 외래키 매핑
  private String conditionValue;
  private Integer priority;
}
