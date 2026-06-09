package com.ssafy.zipdaum.user.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto extends BaseDto {
  private Long id;
  private String email;
  private String password;
  private String name;
}
