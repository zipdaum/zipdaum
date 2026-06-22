package com.ssafy.zipdaum.recommendation.api;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.recommendation.config.AiSummaryProperties;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GmsOpenAiClient {

  private final RestClient restClient;
  private final AiSummaryProperties properties;

  public GmsOpenAiClient(RestClient.Builder builder, AiSummaryProperties properties) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(properties.getConnectTimeout());
    requestFactory.setReadTimeout(properties.getReadTimeout());
    this.restClient = builder
        .baseUrl(properties.getBaseUrl())
        .requestFactory(requestFactory)
        .build();
    this.properties = properties;
  }

  public String summarize(String userPrompt) {
    if (!properties.hasApiKey()) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }

    ChatCompletionRequest request = new ChatCompletionRequest(
        properties.getModel(),
        List.of(
            new ChatMessage("developer", "Answer in Korean. Keep the answer to one natural sentence."),
            new ChatMessage("user", userPrompt)
        )
    );

    try {
      ChatCompletionResponse response = restClient.post()
          .uri(properties.getChatCompletionsPath())
          .contentType(MediaType.APPLICATION_JSON)
          .header("Authorization", "Bearer " + properties.getApiKey())
          .body(request)
          .retrieve()
          .body(ChatCompletionResponse.class);

      return extractSummary(response);
    } catch (RestClientException e) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
  }

  private String extractSummary(ChatCompletionResponse response) {
    if (response == null || response.choices() == null || response.choices().isEmpty()) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }

    ChatMessage message = response.choices().getFirst().message();
    if (message == null || message.content() == null || message.content().isBlank()) {
      throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
    }
    return message.content().trim();
  }

  private record ChatCompletionRequest(String model, List<ChatMessage> messages) {
  }

  private record ChatCompletionResponse(List<Choice> choices) {
  }

  private record Choice(ChatMessage message) {
  }

  private record ChatMessage(String role, String content) {
  }
}
