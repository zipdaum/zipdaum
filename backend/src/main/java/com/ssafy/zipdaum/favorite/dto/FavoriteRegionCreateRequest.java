package com.ssafy.zipdaum.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관심 지역 등록 요청")
public class FavoriteRegionCreateRequest {

  @Schema(description = "법정동 코드 앞 5자리", example = "26350")
  @NotBlank
  @Pattern(regexp = "\\d{5}")
  private String sggCd;

  @Schema(description = "읍면동명", example = "우동")
  @NotBlank
  private String umdNm;
}
