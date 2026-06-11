package com.ssafy.zipdaum.property.dto;

import java.math.BigDecimal;

public record CoordinateDto(
    String addressName,
    BigDecimal latitude,
    BigDecimal longitude
) {
}
