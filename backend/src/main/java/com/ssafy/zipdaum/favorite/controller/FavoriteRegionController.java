package com.ssafy.zipdaum.favorite.controller;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionCreateRequest;
import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import com.ssafy.zipdaum.favorite.service.FavoriteRegionService;
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
@RequestMapping("/users/info/regions")
@RequiredArgsConstructor
@Tag(name = "관심 지역", description = "관심 지역 관리 API")
public class FavoriteRegionController {

  private final FavoriteRegionService favoriteRegionService;

  @GetMapping
  @Operation(summary = "관심 지역 조회", description = "현재 로그인한 사용자의 관심 지역 거래 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "관심 지역 조회 성공"),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<FavoriteRegionResponse>> getFavoriteRegions(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("GET /users/info/regions 요청");

    return ResponseEntity.ok(
        favoriteRegionService.findFavoriteRegions(authenticatedUser.getId())
    );
  }

  @PostMapping
  @Operation(summary = "관심 지역 등록", description = "현재 로그인한 사용자의 관심 지역을 등록합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "관심 지역 등록 성공", content = @Content),
      @ApiResponse(responseCode = "400", description = "입력값 오류 또는 이미 등록된 관심 지역", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> addFavoriteRegion(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody FavoriteRegionCreateRequest request
  ) {
    log.info("POST /users/info/regions 요청");

    favoriteRegionService.saveFavoriteRegion(
        authenticatedUser.getId(),
        request.getSggCd(),
        request.getUmdNm()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body("관심 지역 등록 성공");
  }

  @DeleteMapping
  @Operation(summary = "관심 지역 해제", description = "현재 로그인한 사용자의 관심 지역을 해제합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "관심 지역 해제 성공", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "관심 목록에서 지역 정보를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> deleteFavoriteRegion(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @RequestParam String sggCd,
      @RequestParam String umdNm
  ) {
    log.info("DELETE /users/info/regions 요청");

    favoriteRegionService.removeFavoriteRegion(authenticatedUser.getId(), sggCd, umdNm);

    return ResponseEntity.ok("관심 지역 해제 성공");
  }
}
