package com.ssafy.zipdaum.auth.service;

import com.ssafy.zipdaum.auth.dto.AuthRequest;
import com.ssafy.zipdaum.auth.dto.AuthResponse;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.global.security.JwtTokenProvider;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenProvider jwtTokenProvider;

  @Override
  @Transactional(readOnly = true)
  public AuthResponse login(AuthRequest authRequest) {
    log.info("로그인 요청 email={}", authRequest.getEmail());

    UserDto user = userMapper.findByEmail(authRequest.getEmail());

    if (user == null) {
      log.warn("존재하지 않는 사용자 email={}", authRequest.getEmail());
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    if (!passwordEncoder.matches(authRequest.getPassword(), user.getPassword())) {
      log.warn("비밀번호 불일치 email={}", authRequest.getEmail());
      throw new BusinessException(ErrorCode.INVALID_PASSWORD);
    }

    String accessToken = jwtTokenProvider.createAccessToken(user);

    log.info("로그인 완료 email={}", authRequest.getEmail());
    return new AuthResponse(accessToken, user.getId(), user.getEmail(), user.getName());
  }
}
