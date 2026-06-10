package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleDealSaveCommand(
    Long propertyId,
    BigDecimal exclusiveArea,
    BigDecimal landArea,
    Long dealAmount,
    Integer floor,
    LocalDate dealDate,
    String buyerGbn,
    String sellerGbn
) {
}
