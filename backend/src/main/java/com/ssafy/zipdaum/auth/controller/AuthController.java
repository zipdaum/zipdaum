package com.ssafy.zipdaum.auth.controller;

import com.ssafy.zipdaum.auth.dto.AuthRequest;
import com.ssafy.zipdaum.auth.dto.AuthResponse;
import com.ssafy.zipdaum.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인 및 로그아웃 API")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 JWT access token을 발급합니다.")
  @ApiResponses({
      @ApiResponse(
          responseCode = "200",
          description = "로그인 성공",
          content = @Content(schema = @Schema(implementation = AuthResponse.class))
      ),
      @ApiResponse(responseCode = "400", description = "이메일 또는 비밀번호가 올바르지 않음", content = @Content),
      @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음", content = @Content)
  })
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest) {
    log.info("POST /auth/login 요청 email={}", authRequest.getEmail());
    AuthResponse authResponse = authService.login(authRequest);
    return ResponseEntity.ok(authResponse);
  }

  @PostMapping("/logout")
  @Operation(summary = "로그아웃", description = "클라이언트가 토큰을 삭제하는 로그아웃 처리를 위한 응답을 반환합니다.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content)
  })
  public ResponseEntity<String> logout() {
    log.info("POST /auth/logout 요청");
    return ResponseEntity.ok("로그아웃 성공");
  }
}
