package com.ssafy.zipdaum.recent.controller;

import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import com.ssafy.zipdaum.recent.service.RecentPropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users/info/recent-properties")
@RequiredArgsConstructor
@Tag(name = "최근 본 주택", description = "최근 본 주택 조회 API")
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
}
