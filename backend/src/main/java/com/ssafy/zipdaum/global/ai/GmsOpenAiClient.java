package com.ssafy.zipdaum.global.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class GmsOpenAiClient {

  private final RestClient restClient;
  private final GmsOpenAiProperties properties;
  private final ObjectMapper objectMapper;

  public GmsOpenAiClient(
      RestClient.Builder builder,
      GmsOpenAiProperties properties,
      ObjectMapper objectMapper) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeout());
    requestFactory.setReadTimeout(properties.getReadTimeout());
    this.restClient = builder
        .baseUrl(properties.getBaseUrl())
        .requestFactory(requestFactory)
        .build();
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public String chatCompletion(String developerPrompt, String userPrompt) {
    return chatCompletion(developerPrompt, userPrompt, null);
  }

  public String chatCompletion(String developerPrompt, String userPrompt, String logName) {
    if (!properties.hasApiKey()) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }

    GenerateContentRequest request = new GenerateContentRequest(
        List.of(new Content(List.of(new Part(developerPrompt + System.lineSeparator() + userPrompt))))
    );

    long startedAt = System.nanoTime();
    try {
      byte[] rawResponse = restClient.post()
          .uri(properties.getGenerateContentPath().formatted(properties.getModel()))
          .contentType(MediaType.APPLICATION_JSON)
          .accept(MediaType.APPLICATION_JSON)
          .header("x-goog-api-key", properties.getApiKey())
          .body(request)
          .retrieve()
          .body(byte[].class);

      if (logName != null && !logName.isBlank()) {
        log.info("AI {} HTTP elapsedMs={}, responseBytes={}",
            logName,
            elapsedMillis(startedAt),
            rawResponse == null ? 0 : rawResponse.length);
      }

      long parseStartedAt = System.nanoTime();
      String content = extractContent(rawResponse);
      if (logName != null && !logName.isBlank()) {
        log.info("AI {} response parse elapsedMs={}, contentLength={}",
            logName,
            elapsedMillis(parseStartedAt),
            content.length());
      }
      return content;
    } catch (RestClientException e) {
      log.warn("AI API 호출 실패 logName={}", logName, e);
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private String extractContent(byte[] rawResponse) {
    try {
      JsonNode response = objectMapper.readTree(new String(rawResponse, StandardCharsets.UTF_8));
      String content = response == null
          ? null
          : response.path("candidates").path(0).path("content").path("parts").path(0)
              .path("text").asText(null);
      if (content == null || content.isBlank()) {
        throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
      }
      return content;
    } catch (BusinessException e) {
      throw e;
    } catch (Exception e) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private long elapsedMillis(long startedAt) {
    return (System.nanoTime() - startedAt) / 1_000_000;
  }

  private record GenerateContentRequest(List<Content> contents) {
  }

  private record Content(List<Part> parts) {
  }

  private record Part(String text) {
  }
}
