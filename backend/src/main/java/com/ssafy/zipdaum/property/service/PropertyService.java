package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.dto.PropertyDealHistoryResponse;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import java.util.List;

public interface PropertyService {

  List<PropertySearchResponse> searchProperties(PropertySearchRequest request);

  PropertyDetailResponse findPropertyDetail(Long propertyId);

  PropertyDealHistoryResponse findPropertyDealHistories(Long propertyId);
}
