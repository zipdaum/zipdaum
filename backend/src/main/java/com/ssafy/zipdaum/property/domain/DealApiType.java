package com.ssafy.zipdaum.property.domain;

import lombok.Getter;

@Getter
public enum DealApiType {
  APARTMENT_SALE(
      "APARTMENT",
      true,
      "https://apis.data.go.kr/1613000/RTMSDataSvcAptTrade/getRTMSDataSvcAptTrade"
  ),
  VILLA_SALE(
      "VILLA",
      true,
      "https://apis.data.go.kr/1613000/RTMSDataSvcRHTrade/getRTMSDataSvcRHTrade"
  ),
  APARTMENT_RENT(
      "APARTMENT",
      false,
      "https://apis.data.go.kr/1613000/RTMSDataSvcAptRent/getRTMSDataSvcAptRent"
  ),
  VILLA_RENT(
      "VILLA",
      false,
      "https://apis.data.go.kr/1613000/RTMSDataSvcRHRent/getRTMSDataSvcRHRent"
  );

  private final String propertyType;
  private final boolean sale;
  private final String url;

  DealApiType(String propertyType, boolean sale, String url) {
    this.propertyType = propertyType;
    this.sale = sale;
    this.url = url;
  }
}
