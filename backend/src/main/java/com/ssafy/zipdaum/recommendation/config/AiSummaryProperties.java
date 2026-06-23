package com.ssafy.zipdaum.recommendation.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai.summary")
public class AiSummaryProperties {

  private String baseUrl;
  private String chatCompletionsPath;
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

  public String getChatCompletionsPath() {
    return chatCompletionsPath;
  }

  public void setChatCompletionsPath(String chatCompletionsPath) {
    this.chatCompletionsPath = chatCompletionsPath;
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
