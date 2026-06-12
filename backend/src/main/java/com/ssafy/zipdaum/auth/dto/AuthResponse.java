package com.ssafy.zipdaum.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "로그인 응답")
public class AuthResponse {
  @Schema(description = "JWT access token")
  private final String accessToken;
  @Schema(description = "회원 ID", example = "1")
  private final Long userId;
  @Schema(description = "이메일", example = "user@example.com")
  private final String email;
  @Schema(description = "이름", example = "홍길동")
  private final String name;
}
