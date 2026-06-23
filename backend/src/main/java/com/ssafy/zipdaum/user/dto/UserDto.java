package com.ssafy.zipdaum.user.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원 정보")
public class UserDto extends BaseDto {
  @Schema(description = "회원 ID", example = "1")
  private Long id;
  @Schema(description = "이메일", example = "user@example.com")
  private String email;
  @Schema(description = "비밀번호", example = "password1234")
  private String password;
  @Schema(description = "이름", example = "홍길동")
  private String name;
  @Schema(description = "권한", example = "ROLE_USER")
  String role;
  @Schema(description = "회원 정보 물리 삭제 예정 시각")
  private LocalDateTime deletionScheduledAt;
}
