package com.ssafy.zipdaum.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원 정보 수정 요청")
public class UserUpdateRequest {
  @Schema(description = "수정할 이름", example = "김집다움")
  @NotBlank
  @Size(max = 30)
  private String name;
}
