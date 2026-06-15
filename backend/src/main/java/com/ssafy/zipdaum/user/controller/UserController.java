package com.ssafy.zipdaum.user.controller;

import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import com.ssafy.zipdaum.user.dto.UserInfoResponse;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.dto.UserSignUpRequest;
import com.ssafy.zipdaum.user.dto.UserUpdateRequest;
import com.ssafy.zipdaum.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "회원", description = "회원가입, 회원 정보 조회/수정/탈퇴 API")
public class UserController {
  private final UserService userService;

  @PostMapping
  @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름으로 회원가입을 진행합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content),
      @ApiResponse(responseCode = "400", description = "중복 이메일 또는 입력값 오류", content = @Content)
  })
  public ResponseEntity<String> signUp(@Valid @RequestBody UserSignUpRequest request) {
    log.info("POST /users 요청");
    userService.signUp(request);
    return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
  }

  @GetMapping("/info")
  @Operation(summary = "회원 정보 조회", description = "현재 로그인한 사용자의 회원 정보를 조회합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "회원 정보 조회 성공",
          content = @Content(schema = @Schema(implementation = UserInfoResponse.class))
      ),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<UserInfoResponse> getUserInfo(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("GET /users/info 요청");

    UserDto user = userService.findById(authenticatedUser.getId());
    UserInfoResponse response = new UserInfoResponse(
        user.getId(),
        user.getEmail(),
        user.getName(),
        user.getCreatedAt(),
        user.getUpdatedAt()
    );

    return ResponseEntity.ok(response);
  }

  @PatchMapping("/info")
  @Operation(summary = "회원 정보 수정", description = "현재 로그인한 사용자의 이름을 수정합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "회원 정보 수정 성공", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> updateUserInfo(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
      @Valid @RequestBody UserUpdateRequest userUpdateRequest
  ) {
    log.info("PATCH /users/info 요청");
    userService.updateName(authenticatedUser.getId(), userUpdateRequest.getName());
    return ResponseEntity.ok("회원 정보 수정 성공");
  }

  @DeleteMapping("/info")
  @Operation(summary = "회원 탈퇴", description = "현재 로그인한 사용자를 논리 삭제 처리합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공", content = @Content),
      @ApiResponse(responseCode = "401", description = "인증 실패", content = @Content),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
  })
  @SecurityRequirement(name = "bearerAuth")
  public ResponseEntity<String> deleteUser(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("DELETE /users/info 요청");
    userService.deleteById(authenticatedUser.getId());
    return ResponseEntity.ok("회원 탈퇴 성공");
  }
}
