package com.ssafy.zipdaum.global.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.gms")
public class GmsOpenAiProperties {

  private String baseUrl;
  private String generateContentPath;
  private String apiKey;
  private String model;
  private Duration connectTimeout;
  private Duration readTimeout;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getGenerateContentPath() {
    return generateContentPath;
  }

  public void setGenerateContentPath(String generateContentPath) {
    this.generateContentPath = generateContentPath;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public boolean hasApiKey() {
    return apiKey != null && !apiKey.isBlank();
  }

  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
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
