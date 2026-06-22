package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RentDealSaveCommand(
    Long propertyId,
    BigDecimal exclusiveArea,
    BigDecimal landArea,
    Long deposit,
    Long monthlyRent,
    Integer floor,
    String contractTerm,
    String contractType,
    Boolean useRrRight,
    Long preDeposit,
    Long preMonthlyRent,
    LocalDate dealDate
) {
}
