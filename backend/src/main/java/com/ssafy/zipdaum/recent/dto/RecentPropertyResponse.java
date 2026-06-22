package com.ssafy.zipdaum.recent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "최근 본 주택 조회 응답")
public class RecentPropertyResponse {

  private Long propertyId;
  private String propertyType;
  private String name;
  private String sggCd;
  private String umdNm;
  private String jibun;
  private Integer buildYear;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private Long latestSalePrice;
  private Long latestDeposit;
  private Long latestMonthlyRent;
  private Integer viewCount;
  private LocalDateTime viewedAt;
}
