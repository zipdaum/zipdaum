package com.ssafy.zipdaum.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.auth.dto.AuthRequest;
import com.ssafy.zipdaum.auth.dto.AuthResponse;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.global.security.JwtTokenProvider;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

  private final UserMapper userMapper = mock(UserMapper.class);
  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
  private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
  private final AuthServiceImpl service = new AuthServiceImpl(userMapper, passwordEncoder, jwtTokenProvider);

  @Test
  void login_성공하면_JWT를_발급하고_사용자_정보를_반환한다() {
    AuthRequest request = new AuthRequest();
    request.setEmail("user@example.com");
    request.setPassword("password1234");

    UserDto user = new UserDto();
    user.setId(1L);
    user.setEmail("user@example.com");
    user.setPassword("encoded-password");
    user.setName("홍길동");

    given(userMapper.findByEmail("user@example.com")).willReturn(user);
    given(passwordEncoder.matches("password1234", "encoded-password")).willReturn(true);
    given(jwtTokenProvider.createAccessToken(user)).willReturn("access-token");

    AuthResponse response = service.login(request);

    assertThat(response.getAccessToken()).isEqualTo("access-token");
    assertThat(response.getUserId()).isEqualTo(1L);
    assertThat(response.getEmail()).isEqualTo("user@example.com");
    assertThat(response.getName()).isEqualTo("홍길동");
    then(userMapper).should().findByEmail("user@example.com");
    then(passwordEncoder).should().matches("password1234", "encoded-password");
    then(jwtTokenProvider).should().createAccessToken(user);
  }

  @Test
  void login_존재하지_않는_이메일이면_USER_NOT_FOUND_예외가_발생한다() {
    AuthRequest request = new AuthRequest();
    request.setEmail("missing@example.com");
    request.setPassword("password1234");

    given(userMapper.findByEmail("missing@example.com")).willReturn(null);

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND)
        );

    then(passwordEncoder).shouldHaveNoInteractions();
    then(jwtTokenProvider).shouldHaveNoInteractions();
  }

  @Test
  void login_비밀번호가_일치하지_않으면_INVALID_CREDENTIALS_예외가_발생한다() {
    AuthRequest request = new AuthRequest();
    request.setEmail("user@example.com");
    request.setPassword("wrong-password");

    UserDto user = new UserDto();
    user.setId(1L);
    user.setEmail("user@example.com");
    user.setPassword("encoded-password");
    user.setName("홍길동");

    given(userMapper.findByEmail("user@example.com")).willReturn(user);
    given(passwordEncoder.matches("wrong-password", "encoded-password")).willReturn(false);

    assertThatThrownBy(() -> service.login(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_CREDENTIALS)
        );

    then(jwtTokenProvider).shouldHaveNoInteractions();
  }
}
