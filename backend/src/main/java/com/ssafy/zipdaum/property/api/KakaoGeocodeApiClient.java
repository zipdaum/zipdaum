package com.ssafy.zipdaum.property.api;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.config.KakaoApiProperties;
import com.ssafy.zipdaum.property.dto.CoordinateDto;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class KakaoGeocodeApiClient {

  private final RestClient restClient;
  private final KakaoApiProperties properties;

  public CoordinateDto fetchCoordinate(String address) {
    Map<?, ?> response = restClient.get()
        .uri(URI.create("https://dapi.kakao.com/v2/local/search/address.json"
            + "?query=" + URLEncoder.encode(address, StandardCharsets.UTF_8)
            + "&size=1"))
        .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + properties.getRestApiKey())
        .retrieve()
        .body(Map.class);

    if (response == null) {
      throw new BusinessException(ErrorCode.COORDINATE_NOT_FOUND);
    }

    Object documentsValue = response.get("documents");
    if (!(documentsValue instanceof List<?> documents) || documents.isEmpty()) {
      throw new BusinessException(ErrorCode.COORDINATE_NOT_FOUND);
    }

    Object firstValue = documents.getFirst();
    if (!(firstValue instanceof Map<?, ?> first)) {
      throw new BusinessException(ErrorCode.COORDINATE_NOT_FOUND);
    }

    Object longitude = first.get("x");
    Object latitude = first.get("y");
    Object addressName = first.get("address_name");
    if (longitude == null || latitude == null) {
      throw new BusinessException(ErrorCode.COORDINATE_NOT_FOUND);
    }

    return new CoordinateDto(
        addressName == null ? address : addressName.toString(),
        new BigDecimal(latitude.toString()),
        new BigDecimal(longitude.toString())
    );
  }
}
