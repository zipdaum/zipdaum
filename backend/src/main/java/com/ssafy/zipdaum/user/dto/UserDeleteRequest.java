package com.ssafy.zipdaum.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원 탈퇴 요청")
public class UserDeleteRequest {

  @NotBlank
  @Schema(description = "현재 사용자 이름", example = "홍길동")
  private String name;

  @NotBlank
  @Schema(description = "탈퇴 확인 문구", example = "delete/홍길동")
  private String confirmationText;
}
