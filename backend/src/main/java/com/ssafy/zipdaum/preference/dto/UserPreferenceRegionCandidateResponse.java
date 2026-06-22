package com.ssafy.zipdaum.preference.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "사용자 맞춤 지역 조건 후보 검색 응답")
public class UserPreferenceRegionCandidateResponse {

  private String sggCd;
  private String sggNm;
  private String umdCd;
  private String umdNm;
  private String displayName;
}
