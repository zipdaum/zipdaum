package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.KakaoGeocodeApiClient;
import com.ssafy.zipdaum.property.config.KakaoApiProperties;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoGeocodeServiceImpl implements GeocodeService {

  private final KakaoGeocodeApiClient kakaoGeocodeApiClient;
  private final KakaoApiProperties properties;

  @Override
  public CoordinateDto getCoordinate(String address) {
    if (!properties.hasRestApiKey()) {
      log.warn("좌표 조회 실패 - 카카오 API 키 누락");
      throw new BusinessException(ErrorCode.KAKAO_API_KEY_NOT_FOUND);
    }
    if (address == null || address.isBlank()) {
      log.warn("좌표 조회 실패 - 빈 주소");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    try {
      return kakaoGeocodeApiClient.fetchCoordinate(address.trim());
    } catch (BusinessException e) {
      if (e.getErrorCode() == ErrorCode.COORDINATE_NOT_FOUND) {
        log.warn("좌표 조회 실패 - 좌표 정보 없음");
      }
      throw e;
    }
  }
}
