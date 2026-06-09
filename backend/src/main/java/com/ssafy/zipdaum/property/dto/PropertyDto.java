package com.ssafy.zipdaum.property.dto;

import com.ssafy.zipdaum.global.dto.BaseDto;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
public class PropertyDto extends BaseDto {
  private Long id;
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
}
