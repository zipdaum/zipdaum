package com.ssafy.zipdaum.recent.controller;

import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import com.ssafy.zipdaum.recent.dto.RecentPropertySaveRequest;
import com.ssafy.zipdaum.recent.service.RecentPropertyService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users/info/recent-properties")
@RequiredArgsConstructor
@Tag(name = "최근 본 주택", description = "최근 본 주택 저장 및 조회 API")
public class RecentPropertyController {

  private final RecentPropertyService recentPropertyService;

  @GetMapping
  @Operation(summary = "최근 본 주택 조회", description = "현재 로그인한 사용자의 최근 본 주택을 최신순으로 최대 30개 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "최근 본 주택 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<RecentPropertyResponse>> getRecentProperties(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("GET /users/info/recent-properties 요청");

    return ResponseEntity.ok(recentPropertyService.findRecentProperties(authenticatedUser.getId()));
  }

  @PostMapping
  @Operation(summary = "최근 본 주택 저장", description = "현재 로그인한 사용자의 최근 본 주택을 저장합니다. 같은 주택은 최신 방문으로 갱신합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "최근 본 주택 저장 성공", content = @Content),
      @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "주택 또는 거래 정보를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> saveRecentProperty(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody RecentPropertySaveRequest request
  ) {
    log.info("POST /users/info/recent-properties 요청");

    recentPropertyService.saveRecentProperty(authenticatedUser.getId(), request);

    return ResponseEntity.status(HttpStatus.CREATED).body("최근 본 주택 저장 성공");
  }
}
