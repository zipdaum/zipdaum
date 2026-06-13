package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PropertyDetailResponse {

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
  private List<PropertySaleDealResponse> saleDeals;
  private List<PropertyRentDealResponse> rentDeals;
}
