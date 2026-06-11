package com.ssafy.zipdaum.property.dto;

import com.ssafy.zipdaum.property.domain.DealApiType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record RealEstateDealItem(
    DealApiType apiType,
    String sggCd,
    String umdNm,
    String jibun,
    String propertyName,
    Integer buildYear,
    BigDecimal exclusiveArea,
    BigDecimal landArea,
    Long dealAmount,
    Long deposit,
    Long monthlyRent,
    Integer floor,
    String cdealType,
    String cdealDay,
    String dealingGbn,
    String estateAgentSggNm,
    String rgstDate,
    String aptDong,
    String buyerGbn,
    String sellerGbn,
    String landLeaseholdGbn,
    String contractTerm,
    String contractType,
    Boolean useRrRight,
    Long preDeposit,
    Long preMonthlyRent,
    LocalDate dealDate
) {
}
