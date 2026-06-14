package com.ssafy.zipdaum.property.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PropertyDealHistoryResponse {

  private final List<PropertySaleDealResponse> saleDeals;
  private final List<PropertyRentDealResponse> rentDeals;
}
