package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.global.util.RedisUtil;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.mapper.UserMapper;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @InjectMocks
    private EmailService emailService;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private RedisUtil redisUtil;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "configEmail", "test@example.com");
    }

    @Test
    @DisplayName("인증 코드 전송 성공")
    void sendVerificationCode_Success() throws Exception {
        // given
        String toEmail = "user@example.com";
        when(userMapper.findByEmail(toEmail)).thenReturn(null);
        when(redisUtil.exists(toEmail)).thenReturn(false);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail);

        // then
        verify(userMapper, times(1)).findByEmail(toEmail);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(redisUtil, times(1)).setDataWithTTL(eq(toEmail), anyString(), eq(180L));
    }
    
    @Test
    @DisplayName("인증 코드 전송 성공 - 기존 코드 삭제")
    void sendVerificationCode_Success_DeleteExisting() throws Exception {
        // given
        String toEmail = "user@example.com";
        when(userMapper.findByEmail(toEmail)).thenReturn(null);
        when(redisUtil.exists(toEmail)).thenReturn(true);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // when
        emailService.sendVerificationCode(toEmail);

        // then
        verify(userMapper, times(1)).findByEmail(toEmail);
        verify(redisUtil, times(1)).delete(toEmail);
        verify(mailSender, times(1)).send(any(MimeMessage.class));
        verify(redisUtil, times(1)).setDataWithTTL(eq(toEmail), anyString(), eq(180L));
    }

    @Test
    @DisplayName("인증 코드 전송 실패 - 이메일 중복")
    void sendVerificationCode_Fail_DuplicatedEmail() {
        // given
        String toEmail = "user@example.com";
        when(userMapper.findByEmail(toEmail)).thenReturn(new UserDto());

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> emailService.sendVerificationCode(toEmail));
        assertEquals(ErrorCode.DUPLICATED_EMAIL, exception.getErrorCode());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("이메일 코드 검증 성공")
    void verifyEmailCode_Success() {
        // given
        String email = "user@example.com";
        String code = "123456";
        when(redisUtil.getData(email)).thenReturn(code);

        // when
        emailService.verifyEmailCode(email, code);

        // then
        verify(redisUtil, times(1)).getData(email);
        verify(redisUtil, times(1)).delete(email);
        verify(redisUtil, times(1)).setDataWithTTL("SIGNUP_VERIFIED:" + email, "TRUE", 600L);
    }

    @Test
    @DisplayName("이메일 코드 검증 실패 - 코드 불일치")
    void verifyEmailCode_Fail_InvalidCode() {
        // given
        String email = "user@example.com";
        String correctCode = "123456";
        String wrongCode = "654321";
        when(redisUtil.getData(email)).thenReturn(correctCode);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> emailService.verifyEmailCode(email, wrongCode));
        assertEquals(ErrorCode.INVALID_INPUT_CODE, exception.getErrorCode());
    }

    @Test
    @DisplayName("이메일 코드 검증 실패 - 시간 초과")
    void verifyEmailCode_Fail_Timeout() {
        // given
        String email = "user@example.com";
        String code = "123456";
        when(redisUtil.getData(email)).thenReturn(null);

        // when & then
        BusinessException exception = assertThrows(BusinessException.class, () -> emailService.verifyEmailCode(email, code));
        assertEquals(ErrorCode.REQUEST_TIME_OUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("이메일 인증 상태 확인 - 인증됨")
    void checkEmailVerified_True() {
        // given
        String email = "user@example.com";
        when(redisUtil.getData("SIGNUP_VERIFIED:" + email)).thenReturn("TRUE");

        // when
        boolean isVerified = emailService.checkEmailVerified(email);

        // then
        assertTrue(isVerified);
    }

    @Test
    @DisplayName("이메일 인증 상태 확인 - 인증 안됨")
    void checkEmailVerified_False() {
        // given
        String email = "user@example.com";
        when(redisUtil.getData("SIGNUP_VERIFIED:" + email)).thenReturn(null);

        // when
        boolean isVerified = emailService.checkEmailVerified(email);

        // then
        assertFalse(isVerified);
    }



    @Test
    @DisplayName("인증 상태 삭제")
    void deleteVerifiedState() {
        // given
        String email = "user@example.com";

        // when
        emailService.deleteVerifiedState(email);

        // then
        verify(redisUtil, times(1)).delete("SIGNUP_VERIFIED:" + email);
    }
    
    @Test
    @DisplayName("Redis 키 삭제")
    void delete() {
        // given
        String key = "somekey";

        // when
        emailService.delete(key);

        // then
        verify(redisUtil, times(1)).delete(key);
    }
}
