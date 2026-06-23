package com.ssafy.zipdaum.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "주택 AI 적합도 요약 응답")
public class PropertyAiSummaryResponse {

  @Schema(
      description = "주택 상세 제목 하단에 표시할 AI 요약 문장",
      example = "이 집은 전세 예산에는 맞지만, 선호 면적보다 조금 작아요."
  )
  private String summary;
}
