package com.ssafy.zipdaum.preference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "사용자 맞춤 조건 항목 요청")
public class UserPreferenceItemRequest {

  @Schema(description = "조건 코드", example = "SALE_PRICE")
  @NotBlank
  private String code;

  @Schema(description = "조건값", example = "500000000")
  @NotBlank
  @Size(max = 100)
  private String value;

  @Schema(description = "사용자 지정 우선순위", example = "1")
  @NotNull
  @Positive
  private Integer priority;
}
