package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.RealEstateDealSaveResult;

public interface RealEstateDealFetchService {

  RealEstateDealSaveResult fetchAndSaveDeals(DealApiType apiType, String lawdCd, String dealYmd);
}
