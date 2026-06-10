package com.ssafy.zipdaum.property.service;

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
      throw new IllegalStateException("PUBLIC_DATA_SERVICE_KEY가 설정되지 않았습니다.");
    }
    if (lawdCd == null || !lawdCd.matches("\\d{5}")) {
      throw new IllegalArgumentException("lawdCd는 법정동 코드 앞 5자리여야 합니다.");
    }
    if (dealYmd == null || !dealYmd.matches("\\d{6}")) {
      throw new IllegalArgumentException("dealYmd는 계약년월 6자리여야 합니다. 예: 202501");
    }
    return dealApiClient.fetch(apiType, lawdCd, dealYmd);
  }
}
