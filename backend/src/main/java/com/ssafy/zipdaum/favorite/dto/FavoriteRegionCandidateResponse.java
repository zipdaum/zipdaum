package com.ssafy.zipdaum.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관심 지역 등록 후보 검색 응답")
public class FavoriteRegionCandidateResponse {

  private String sggCd;
  private String umdNm;
  private String displayName;
}
