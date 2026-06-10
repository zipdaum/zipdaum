package com.ssafy.zipdaum.property.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property.kakao")
public class KakaoApiProperties {

  private String restApiKey;

  public String getRestApiKey() {
    return restApiKey;
  }

  public void setRestApiKey(String restApiKey) {
    this.restApiKey = restApiKey;
  }

  public boolean hasRestApiKey() {
    return restApiKey != null && !restApiKey.isBlank();
  }
}
