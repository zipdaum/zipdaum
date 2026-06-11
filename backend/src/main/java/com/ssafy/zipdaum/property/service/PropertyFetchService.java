package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.PropertySaveResult;

public interface PropertyFetchService {

  PropertySaveResult fetchAndSaveProperties(DealApiType apiType, String lawdCd, String dealYmd);
}
