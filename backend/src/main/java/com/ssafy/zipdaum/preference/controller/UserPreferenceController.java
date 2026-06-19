package com.ssafy.zipdaum.preference.controller;

import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users/info/preferences")
@RequiredArgsConstructor
@Tag(name = "사용자 맞춤 조건", description = "사용자 맞춤 조건 관리 API")
public class UserPreferenceController {

  private final UserPreferenceService userPreferenceService;

  @GetMapping
  @Operation(summary = "사용자 맞춤 조건 조회", description = "현재 로그인한 사용자의 맞춤 조건을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "맞춤 조건 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "설정된 맞춤 조건 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<UserPreferenceResponse>> getPreferences(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("GET /users/info/preferences 요청");

    return ResponseEntity.ok(userPreferenceService.findPreferences(authenticatedUser.getId()));
  }

  @PutMapping
  @Operation(summary = "사용자 맞춤 조건 등록 및 수정", description = "현재 로그인한 사용자의 맞춤 조건을 등록하거나 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "맞춤 조건 저장 성공", content = @Content),
      @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> savePreferences(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody UserPreferenceRequest request
  ) {
    log.info("PUT /users/info/preferences 요청");

    userPreferenceService.savePreferences(authenticatedUser.getId(), request);

    return ResponseEntity.ok("맞춤 조건 저장 성공");
  }

  @DeleteMapping
  @Operation(summary = "사용자 맞춤 조건 해제", description = "현재 로그인한 사용자의 맞춤 조건을 해제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "맞춤 조건 해제 성공", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "설정된 맞춤 조건 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> deletePreferences(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("DELETE /users/info/preferences 요청");

    userPreferenceService.removePreferences(authenticatedUser.getId());

    return ResponseEntity.ok("맞춤 조건 해제 성공");
  }
}
