package com.ssafy.zipdaum.preference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "사용자 맞춤 조건 응답")
public class UserPreferenceResponse {

  @Schema(description = "조건 코드", example = "SALE_PRICE")
  private String code;

  @Schema(description = "조건명", example = "매매가")
  private String name;

  @Schema(description = "조건값", example = "500000000")
  private String value;

  @Schema(description = "조건 우선순위", example = "1")
  private Integer priority;
}
