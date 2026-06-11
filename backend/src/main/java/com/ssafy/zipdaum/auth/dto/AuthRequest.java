package com.ssafy.zipdaum.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "로그인 요청")
public class AuthRequest {
  @Schema(description = "이메일", example = "user@example.com")
  private String email;
  @Schema(description = "비밀번호", example = "password1234")
  private String password;
}
