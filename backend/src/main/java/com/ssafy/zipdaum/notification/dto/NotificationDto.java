package com.ssafy.zipdaum.notification.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationDto extends BaseDto {
  private Long id;
  private Long userId; // 외래키 매핑
  private String type;
  private String content;
  private Boolean isRead;
}
