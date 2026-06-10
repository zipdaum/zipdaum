package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
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
public class UserServiceImpl implements UserService{

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public void signUp(UserDto userDto) {

    log.info("회원가입 요청 도착 email={}", userDto.getEmail());

    if (userMapper.findByEmail(userDto.getEmail()) != null) {
      log.warn("중복 이메일 email={}", userDto.getEmail());
      throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
    }

    String encodedPassword = passwordEncoder.encode(userDto.getPassword());
    userDto.setPassword(encodedPassword);

    userMapper.insertUser(userDto);

    log.info("회원가입 완료 email={}", userDto.getEmail());
  }
}
