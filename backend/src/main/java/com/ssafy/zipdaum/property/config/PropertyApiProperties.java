package com.ssafy.zipdaum.property.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property.real-estate")
public class PropertyApiProperties {

  private String serviceKey;
  private Duration connectTimeout;
  private Duration readTimeout;

  public String getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(String serviceKey) {
    this.serviceKey = serviceKey;
  }

  public boolean hasServiceKey() {
    return serviceKey != null && !serviceKey.isBlank();
  }

  public Duration getConnectTimeout() {
    return connectTimeout;
  }

  public void setConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = connectTimeout;
  }

  public Duration getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(Duration readTimeout) {
    this.readTimeout = readTimeout;
  }
}
