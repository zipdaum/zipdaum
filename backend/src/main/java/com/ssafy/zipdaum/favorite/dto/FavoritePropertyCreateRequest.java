package com.ssafy.zipdaum.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관심 주택 등록 요청")
public class FavoritePropertyCreateRequest {

  @Schema(description = "주택 ID", example = "1")
  @NotNull
  @Positive
  private Long propertyId;
}
