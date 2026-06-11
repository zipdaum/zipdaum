package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.api.KakaoGeocodeApiClient;
import com.ssafy.zipdaum.property.config.KakaoApiProperties;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class KakaoGeocodeServiceImplTest {

  private final FakeKakaoGeocodeApiClient kakaoGeocodeApiClient = new FakeKakaoGeocodeApiClient();
  private final KakaoApiProperties properties = new KakaoApiProperties();

  @Test
  void getCoordinate_주소_앞뒤_공백을_제거하고_카카오_API를_호출한다() {
    properties.setRestApiKey("test-key");
    GeocodeService service = new KakaoGeocodeServiceImpl(kakaoGeocodeApiClient, properties);

    CoordinateDto coordinate = service.getCoordinate(" 부산 해운대구 우동 1484 ");

    assertThat(kakaoGeocodeApiClient.requestAddress).isEqualTo("부산 해운대구 우동 1484");
    assertThat(coordinate.latitude()).isEqualByComparingTo("35.1");
    assertThat(coordinate.longitude()).isEqualByComparingTo("129.1");
  }

  @Test
  void getCoordinate_카카오_API_키가_없으면_예외가_발생한다() {
    GeocodeService service = new KakaoGeocodeServiceImpl(kakaoGeocodeApiClient, properties);

    assertThatThrownBy(() -> service.getCoordinate("부산 해운대구 우동 1484"))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.KAKAO_API_KEY_NOT_FOUND)
        );
  }

  @Test
  void getCoordinate_주소가_비어있으면_예외가_발생한다() {
    properties.setRestApiKey("test-key");
    GeocodeService service = new KakaoGeocodeServiceImpl(kakaoGeocodeApiClient, properties);

    assertThatThrownBy(() -> service.getCoordinate(" "))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  private static class FakeKakaoGeocodeApiClient extends KakaoGeocodeApiClient {

    private String requestAddress;

    FakeKakaoGeocodeApiClient() {
      super(null, null);
    }

    @Override
    public CoordinateDto fetchCoordinate(String address) {
      this.requestAddress = address;
      return new CoordinateDto(address, new BigDecimal("35.1"), new BigDecimal("129.1"));
    }
  }
}
