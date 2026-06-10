package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.KakaoGeocodeApiClient;
import com.ssafy.zipdaum.property.config.KakaoApiProperties;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KakaoGeocodeService {

  private final KakaoGeocodeApiClient kakaoGeocodeApiClient;
  private final KakaoApiProperties properties;

  public CoordinateDto getCoordinate(String address) {
    if (!properties.hasRestApiKey()) {
      throw new BusinessException(ErrorCode.KAKAO_API_KEY_NOT_FOUND);
    }
    if (address == null || address.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return kakaoGeocodeApiClient.fetchCoordinate(address.trim());
  }
}
