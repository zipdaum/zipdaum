package com.ssafy.zipdaum.preference.controller;

import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRegionCandidateResponse;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.service.UserPreferenceService;
import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users/info/preferences")
@RequiredArgsConstructor
@Tag(name = "사용자 맞춤 조건", description = "사용자 맞춤 조건 관리 API")
public class UserPreferenceController {

  private final UserPreferenceService userPreferenceService;

  @GetMapping("/regions/candidates")
  @Operation(summary = "사용자 맞춤 지역 조건 후보 검색", description = "사용자 맞춤 지역 조건으로 설정할 수 있는 지역 후보를 검색합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "맞춤 지역 조건 후보 검색 성공"),
      @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<List<UserPreferenceRegionCandidateResponse>> getRegionCandidates(
      @RequestParam String keyword
  ) {
    log.info("GET /users/info/preferences/regions/candidates 요청");

    return ResponseEntity.ok(userPreferenceService.findRegionCandidates(keyword));
  }

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
  @Operation(
      summary = "사용자 맞춤 조건 전체 저장",
      description = "현재 로그인한 사용자의 맞춤 조건 목록 전체를 저장합니다. 기존 맞춤 조건이 있으면 요청 본문 기준으로 교체합니다."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "맞춤 조건 저장 성공", content = @Content),
      @ApiResponse(responseCode = "400", description = "입력값 오류", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> savePreferences(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
          required = true,
          description = "저장할 사용자 맞춤 조건 목록",
          content = @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = UserPreferenceRequest.class),
              examples = @ExampleObject(
                  name = "사용자 맞춤 조건 저장 요청",
                  value = """
                      {
                        "preferences": [
                          {
                            "code": "SALE_PRICE",
                            "value": "500000000",
                            "priority": 1
                          },
                          {
                            "code": "AREA",
                            "value": "84.5",
                            "priority": 2
                          },
                          {
                            "code": "REGION",
                            "value": "해운대구 우동",
                            "priority": 3
                          },
                          {
                            "code": "SUBWAY",
                            "value": "true",
                            "priority": 4
                          }
                        ]
                      }
                      """
              )
          )
      )
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
