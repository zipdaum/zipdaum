package com.ssafy.zipdaum.favorite.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FavoritePropertyDto extends BaseDto {
  private Long id;
  private Long userId;     // 외래키 매핑
  private Long propertyId; // 외래키 매핑
}
