package com.ssafy.zipdaum.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청")
public class UserSignUpRequest {
  @Schema(description = "이메일", example = "user@example.com")
  @NotBlank
  @Email
  @Size(max = 100)
  private String email;
  @Schema(description = "비밀번호", example = "password1234")
  @NotBlank
  @Size(min = 8, max = 100)
  private String password;
  @Schema(description = "이름", example = "홍길동")
  @NotBlank
  @Size(max = 30)
  private String name;
}
