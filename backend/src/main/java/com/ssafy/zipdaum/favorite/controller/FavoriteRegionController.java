package com.ssafy.zipdaum.favorite.controller;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionCreateRequest;
import com.ssafy.zipdaum.favorite.service.FavoriteRegionService;
import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users/info/regions")
@RequiredArgsConstructor
@Tag(name = "관심 지역", description = "관심 지역 관리 API")
public class FavoriteRegionController {

  private final FavoriteRegionService favoriteRegionService;

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
    log.info(
        "POST /users/info/regions 요청 userId={}, sggCd={}, umdNm={}",
        authenticatedUser.getId(),
        request.getSggCd(),
        request.getUmdNm()
    );

    favoriteRegionService.saveFavoriteRegion(
        authenticatedUser.getId(),
        request.getSggCd(),
        request.getUmdNm()
    );

    return ResponseEntity.status(HttpStatus.CREATED).body("관심 지역 등록 성공");
  }
}
