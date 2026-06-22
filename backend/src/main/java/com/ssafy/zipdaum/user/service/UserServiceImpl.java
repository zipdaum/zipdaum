package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.user.dto.UserDeleteRequest;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.dto.UserSignUpRequest;
import com.ssafy.zipdaum.user.mapper.UserMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{

  private static final String DELETE_CONFIRMATION_PREFIX = "delete/";
  private static final int USER_DELETION_GRACE_DAYS = 14;

  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final EmailService emailService;

  @Transactional
  public void signUp(UserSignUpRequest request) {

    if (userMapper.findByEmail(request.getEmail()) != null) {
      log.warn("회원가입 실패 - 중복 이메일");
      throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
    }

    if (!emailService.checkEmailVerified(request.getEmail())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED_EMAIL);
    }

    UserDto userDto = new UserDto();
    userDto.setEmail(request.getEmail());
    userDto.setName(request.getName());
    userDto.setPassword(passwordEncoder.encode(request.getPassword()));

    try {
      userMapper.insertUser(userDto);
      emailService.deleteVerifiedState(userDto.getEmail());
    } catch (DuplicateKeyException e) {
      log.warn("회원가입 실패 - 중복 이메일");
      throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
    }

    log.info("회원가입 완료");
  }

  @Override
  @Transactional(readOnly = true)
  public UserDto findById(Long id) {
    UserDto user = userMapper.findById(id);
    if (user == null) {
      log.warn("존재하지 않는 사용자 userId={}", id);
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    return user;
  }

  @Override
  @Transactional
  public void updateName(Long id, String name) {
    int updatedRows = userMapper.updateNameById(id, name);
    if (updatedRows == 0) {
      log.warn("수정 처리할 대상이 없음 userId={}", id);
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    log.info("회원 정보 수정 완료 userId={}", id);
  }

  @Override
  @Transactional
  public void requestDeletion(Long id, UserDeleteRequest request) {
    UserDto user = findById(id);
    String deleteConfirmationText = DELETE_CONFIRMATION_PREFIX + user.getName();
    if (!user.getName().equals(request.getName())
        || !deleteConfirmationText.equals(request.getConfirmationText())) {
      log.warn("회원 탈퇴 확인 실패 userId={}", id);
      throw new BusinessException(ErrorCode.USER_DELETE_CONFIRMATION_MISMATCH);
    }

    LocalDateTime deletionScheduledAt = LocalDateTime.now().plusDays(USER_DELETION_GRACE_DAYS);
    int updatedRows = userMapper.softDeleteById(id, deletionScheduledAt);
    if (updatedRows == 0) {
      log.warn("탈퇴 처리할 대상이 없음 userId={}", id);
      throw new BusinessException(ErrorCode.USER_NOT_FOUND);
    }

    log.info("회원 탈퇴 신청 완료 userId={}, deletionScheduledAt={}", id, deletionScheduledAt);
  }
}
