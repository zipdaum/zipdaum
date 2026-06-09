package com.ssafy.zipdaum.favorite.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoriteRegionDto extends BaseDto {
  private Long id;
  private Long userId;     // 외래키 매핑
  private String sggCd;
  private String umdNm;
}
