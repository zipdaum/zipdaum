package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.RealEstateDealApiClient;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import com.ssafy.zipdaum.property.dto.RealEstateDealItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RealEstateDealFetchService {

  private final RealEstateDealApiClient dealApiClient;
  private final PropertyApiProperties properties;

  public List<RealEstateDealItem> fetchDeals(DealApiType apiType, String lawdCd, String dealYmd) {
    if (!properties.hasServiceKey()) {
      throw new BusinessException(ErrorCode.REAL_ESTATE_API_KEY_NOT_FOUND);
    }
    if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
      throw new BusinessException(ErrorCode.INVALID_LAWD_CODE);
    }
    if (dealYmd == null || !dealYmd.matches("\\d{6}")) {
      throw new BusinessException(ErrorCode.INVALID_DEAL_YMD);
    }
    return dealApiClient.fetch(apiType, lawdCd, dealYmd);
  }
}
