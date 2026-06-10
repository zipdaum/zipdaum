package com.ssafy.zipdaum.property.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "property.real-estate")
public class PropertyApiProperties {

  private String serviceKey;

  public String getServiceKey() {
    return serviceKey;
  }

  public void setServiceKey(String serviceKey) {
    this.serviceKey = serviceKey;
  }

  public boolean hasServiceKey() {
    return serviceKey != null && !serviceKey.isBlank();
  }
}
