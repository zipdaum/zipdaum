package com.ssafy.zipdaum.property.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.config.PropertyApiProperties;
import com.ssafy.zipdaum.property.domain.DealApiType;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PropertyApiClientTest {

  private final PropertyApiProperties properties = new PropertyApiProperties();

  private PropertyApiClient propertyApiClient;
  private MockRestServiceServer server;

  @BeforeEach
  void setUp() {
    properties.setServiceKey("test-key");
    RestClient.Builder builder = RestClient.builder();
    server = MockRestServiceServer.bindTo(builder).build();
    propertyApiClient = new PropertyApiClient(builder.build(), properties);
  }

  @Test
  void fetch_공공데이터_API_응답이_없으면_타임아웃_예외로_변환한다() {
    server.expect(requestTo(DealApiType.APARTMENT_SALE.getUrl()
            + "?serviceKey=test-key"
            + "&LAWD_CD=26350"
            + "&DEAL_YMD=202501"
            + "&numOfRows=1000"
            + "&pageNo=1"))
        .andRespond(request -> {
          throw new SocketTimeoutException("Read timed out");
        });

    assertThatThrownBy(() -> propertyApiClient.fetch(
        DealApiType.APARTMENT_SALE,
        "26350",
        "202501"
    ))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REAL_ESTATE_API_TIMEOUT)
        );

    server.verify();
  }
}
