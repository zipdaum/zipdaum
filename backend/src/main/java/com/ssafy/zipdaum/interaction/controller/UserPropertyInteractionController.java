package com.ssafy.zipdaum.interaction.controller;

import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionRequest;
import com.ssafy.zipdaum.interaction.service.UserPropertyInteractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/properties/{propertyId}/interactions")
@RequiredArgsConstructor
@Tag(name = "사용자 행동 로그", description = "주택 상세 화면 사용자 행동 로그 API")
public class UserPropertyInteractionController {

  private final UserPropertyInteractionService userPropertyInteractionService;

  @PostMapping
  @Operation(
      summary = "주택 상세 화면 행동 로그 저장",
      description = "현재 로그인한 사용자의 주택 상세 화면 체류시간, 최대 스크롤 깊이, 적합도 상세 보기 및 전체 거래 보러가기 클릭 여부를 저장합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "행동 로그 저장 성공"),
      @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "주택 정보 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<Void> saveInteraction(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Parameter(description = "주택 ID", example = "1", required = true)
      @PathVariable @Positive Long propertyId,
      @Valid @RequestBody UserPropertyInteractionRequest request
  ) {
    log.info("POST /properties/{}/interactions 요청", propertyId);
    userPropertyInteractionService.saveInteraction(
        authenticatedUser.getId(),
        propertyId,
        request
    );
    return ResponseEntity.noContent().build();
  }
}
