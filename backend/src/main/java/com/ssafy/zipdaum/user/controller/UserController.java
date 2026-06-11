package com.ssafy.zipdaum.user.controller;

import com.ssafy.zipdaum.global.security.AuthenticatedUser;
import com.ssafy.zipdaum.user.dto.UserInfoResponse;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.service.UserService;
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
public class UserController {
  private final UserService userService;

  @PostMapping
  public ResponseEntity<String> signUp(@RequestBody UserDto userDto) {
    log.info("POST /users 요청 email={}", userDto.getEmail());
    userService.signUp(userDto);
    return ResponseEntity.status(HttpStatus.CREATED).body("회원가입 성공");
  }

  @GetMapping("/info")
  public ResponseEntity<UserInfoResponse> getUserInfo(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("GET /users/info 요청 userId={}", authenticatedUser.getId());

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

  @DeleteMapping("/info")
  public ResponseEntity<String> deleteUser(
      @AuthenticationPrincipal AuthenticatedUser authenticatedUser
  ) {
    log.info("DELETE /users/info 요청 userId={}", authenticatedUser.getId());
    userService.deleteById(authenticatedUser.getId());
    return ResponseEntity.ok("회원 탈퇴 성공");
  }
}
