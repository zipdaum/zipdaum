package com.ssafy.zipdaum.property.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({
    PropertyApiProperties.class,
    KakaoApiProperties.class,
    PropertyAiProperties.class
})
public class PropertyApiConfig {

  @Bean
  @Primary
  public RestClient restClient(RestClient.Builder builder, PropertyApiProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeout());
    requestFactory.setReadTimeout(properties.getReadTimeout());
    return builder.requestFactory(requestFactory).build();
  }

  @Bean
  public RestClient propertyAiRestClient(RestClient.Builder builder, PropertyAiProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeout());
    requestFactory.setReadTimeout(properties.getReadTimeout());
    return builder.requestFactory(requestFactory).build();
  }
}
