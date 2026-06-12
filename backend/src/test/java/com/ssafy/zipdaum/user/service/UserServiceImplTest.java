package com.ssafy.zipdaum.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.dto.UserSignUpRequest;
import com.ssafy.zipdaum.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

  private final UserMapper userMapper = mock(UserMapper.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final UserServiceImpl service = new UserServiceImpl(userMapper, passwordEncoder);

  @Test
  void signUp_회원가입_성공시_비밀번호를_암호화하여_저장한다() {
    UserSignUpRequest request = new UserSignUpRequest();
    request.setEmail("user@example.com");
    request.setPassword("password1234");
    request.setName("홍길동");

    given(userMapper.findByEmail("user@example.com")).willReturn(null);
    given(passwordEncoder.encode("password1234")).willReturn("encoded-password");

    service.signUp(request);

    then(passwordEncoder).should().encode("password1234");
    then(userMapper).should().insertUser(any(UserDto.class));
  }

  @Test
  void signUp_중복_이메일이면_DUPLICATED_EMAIL_예외가_발생한다() {
    UserSignUpRequest request = new UserSignUpRequest();
    request.setEmail("user@example.com");
    request.setPassword("password1234");
    request.setName("홍길동");

    given(userMapper.findByEmail("user@example.com")).willReturn(new UserDto());

    assertThatThrownBy(() -> service.signUp(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_EMAIL)
        );
  }

  @Test
  void signUp_회원_저장시_중복키가_발생하면_DUPLICATED_EMAIL_예외가_발생한다() {
    UserSignUpRequest request = new UserSignUpRequest();
    request.setEmail("user@example.com");
    request.setPassword("password1234");
    request.setName("홍길동");

    given(userMapper.findByEmail("user@example.com")).willReturn(null);
    given(passwordEncoder.encode("password1234")).willReturn("encoded-password");
    willThrow(new DuplicateKeyException("중복 이메일"))
        .given(userMapper).insertUser(any(UserDto.class));

    assertThatThrownBy(() -> service.signUp(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DUPLICATED_EMAIL)
        );
  }

  @Test
  void findById_존재하는_사용자를_반환한다() {
    UserDto user = new UserDto();
    user.setId(1L);
    user.setEmail("user@example.com");
    user.setName("홍길동");

    given(userMapper.findById(1L)).willReturn(user);

    UserDto result = service.findById(1L);

    assertThat(result).isSameAs(user);
  }

  @Test
  void findById_존재하지_않는_사용자이면_USER_NOT_FOUND_예외가_발생한다() {
    given(userMapper.findById(1L)).willReturn(null);

    assertThatThrownBy(() -> service.findById(1L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
        );
  }

  @Test
  void updateName_성공하면_이름을_수정한다() {
    given(userMapper.updateNameById(1L, "새이름")).willReturn(1);

    service.updateName(1L, "새이름");

    then(userMapper).should().updateNameById(1L, "새이름");
  }

  @Test
  void updateName_수정할_대상이_없으면_USER_NOT_FOUND_예외가_발생한다() {
    given(userMapper.updateNameById(1L, "새이름")).willReturn(0);

    assertThatThrownBy(() -> service.updateName(1L, "새이름"))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
        );
  }

  @Test
  void deleteById_성공하면_논리삭제한다() {
    given(userMapper.softDeleteById(1L)).willReturn(1);

    service.deleteById(1L);

    then(userMapper).should().softDeleteById(1L);
  }

  @Test
  void deleteById_삭제할_대상이_없으면_USER_NOT_FOUND_예외가_발생한다() {
    given(userMapper.softDeleteById(1L)).willReturn(0);

    assertThatThrownBy(() -> service.deleteById(1L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
        );
  }
}
