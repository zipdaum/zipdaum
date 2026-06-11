package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import java.util.List;

public interface PropertyService {

  List<PropertySearchResponse> searchProperties(PropertySearchRequest request);
}
