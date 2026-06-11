package com.ssafy.zipdaum.user.dto;

import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "회원 정보 조회 응답")
public class UserInfoResponse {
  @Schema(description = "회원 ID", example = "1")
  private final Long id;
  @Schema(description = "이메일", example = "user@example.com")
  private final String email;
  @Schema(description = "이름", example = "홍길동")
  private final String name;
  @Schema(description = "생성 시각")
  private final LocalDateTime createdAt;
  @Schema(description = "수정 시각")
  private final LocalDateTime updatedAt;
}
