package com.ssafy.zipdaum.property.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PropertyDealHistoryResponse {

  private final List<PropertySaleDealResponse> saleDeals;
  private final List<PropertyRentDealResponse> rentDeals;
  private final int salePage;
  private final int saleSize;
  private final long saleTotalCount;
  private final int saleTotalPages;
  private final String rentDealType;
  private final int rentPage;
  private final int rentSize;
  private final long rentTotalCount;
  private final int rentTotalPages;
  private final long jeonseTotalCount;
  private final long monthlyRentTotalCount;
}
