package com.ssafy.zipdaum.favorite.controller;

import com.ssafy.zipdaum.favorite.dto.FavoritePropertyCreateRequest;
import com.ssafy.zipdaum.favorite.dto.FavoritePropertyResponse;
import com.ssafy.zipdaum.favorite.service.FavoritePropertyService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users/info/properties")
@RequiredArgsConstructor
@Tag(name = "관심 주택", description = "관심 주택 관리 API")
public class FavoritePropertyController {

  private final FavoritePropertyService favoritePropertyService;

  @GetMapping
  @Operation(summary = "관심 주택 조회", description = "현재 로그인한 사용자의 관심 주택 목록을 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "관심 주택 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<FavoritePropertyResponse>> getFavoriteProperties(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("GET /users/info/properties 요청 userId={}", authenticatedUser.getId());

    return ResponseEntity.ok(
        favoritePropertyService.findFavoriteProperties(authenticatedUser.getId())
    );
  }

  @PostMapping
  @Operation(summary = "관심 주택 등록", description = "현재 로그인한 사용자의 관심 주택을 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "관심 주택 등록 성공", content = @Content),
      @ApiResponse(responseCode = "400", description = "입력값 오류 또는 이미 등록된 관심 주택", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "주택 정보를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> addFavoriteProperty(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody FavoritePropertyCreateRequest request
  ) {
    log.info(
        "POST /users/info/properties 요청 userId={}, propertyId={}",
        authenticatedUser.getId(),
        request.getPropertyId()
    );

    favoritePropertyService.saveFavoriteProperty(
        authenticatedUser.getId(),
        request.getPropertyId()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body("관심 주택 등록 성공");
  }

  @DeleteMapping
  @Operation(summary = "관심 주택 해제", description = "현재 로그인한 사용자의 관심 주택을 해제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "관심 주택 해제 성공", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "관심 목록에서 주택 정보를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> deleteFavoriteProperty(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam Long propertyId
  ) {
    log.info(
        "DELETE /users/info/properties 요청 userId={}, propertyId={}",
        authenticatedUser.getId(),
        propertyId
    );

    favoritePropertyService.removeFavoriteProperty(
        authenticatedUser.getId(),
        propertyId
    );

    return ResponseEntity.ok("관심 주택 해제 성공");
  }
}
