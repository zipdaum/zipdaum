package com.ssafy.zipdaum.auth.controller;

import com.ssafy.zipdaum.auth.dto.AuthRequest;
import com.ssafy.zipdaum.auth.dto.AuthResponse;
import com.ssafy.zipdaum.auth.service.AuthService;
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
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest) {
    log.info("POST /auth/login 요청 email={}", authRequest.getEmail());
    AuthResponse authResponse = authService.login(authRequest);
    return ResponseEntity.ok(authResponse);
  }

  @PostMapping("/logout")
  public ResponseEntity<String> logout() {
    log.info("POST /auth/logout 요청");
    return ResponseEntity.ok("로그아웃 성공");
  }
}
