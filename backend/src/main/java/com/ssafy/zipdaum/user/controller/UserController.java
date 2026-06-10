package com.ssafy.zipdaum.user.controller;

import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.service.UserService;
import com.ssafy.zipdaum.user.service.UserServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
