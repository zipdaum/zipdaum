package com.ssafy.zipdaum.favorite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "관심 지역 조회 응답")
public class FavoriteRegionResponse {

  private String sggCd;
  private String sggNm;
  private String umdCd;
  private String umdNm;
  private String displayName;
  private Long latestSalePrice;
  private Long latestJeonseDeposit;
  private Long latestMonthlyRentDeposit;
  private Long latestMonthlyRent;
  private Long saleDealCount;
  private Long jeonseDealCount;
  private Long monthlyRentDealCount;
}
