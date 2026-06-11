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

  @Override
  @Transactional(readOnly = true)
  public UserDto findById(Long id) {
    log.info("회원 정보 조회 요청 userId={}", id);

    UserDto user = userMapper.findById(id);
    if (user == null) {
      log.warn("존재하지 않는 사용자 userId={}", id);
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    return user;
  }

  @Override
  @Transactional
  public void deleteById(Long id) {
    log.info("회원 탈퇴 요청 userId={}", id);

    int updatedRows = userMapper.softDeleteById(id);
    if (updatedRows == 0) {
      log.warn("탈퇴 처리할 대상이 없음 userId={}", id);
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    log.info("회원 탈퇴 완료 userId={}", id);
  }
}
