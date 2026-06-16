package com.ssafy.zipdaum.preference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "사용자 맞춤 조건 등록 및 수정 요청")
public class UserPreferenceRequest {

  @Schema(description = "사용자 맞춤 조건 목록")
  @Valid
  @NotEmpty
  private List<UserPreferenceItemRequest> preferences;
}
