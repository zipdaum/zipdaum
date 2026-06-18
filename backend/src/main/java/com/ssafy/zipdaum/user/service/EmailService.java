package com.ssafy.zipdaum.user.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.global.util.RedisUtil;
import com.ssafy.zipdaum.user.dto.UserDto;
import com.ssafy.zipdaum.user.mapper.UserMapper;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisUtil redisUtil;
    private final UserMapper userMapper;

    @Value("${MAIL_USERNAME}")
    private String configEmail;


    private String createCode() {
        int code = (int) (Math.random() * 900000) + 100000;

        return String.valueOf(code);
    }

    private MimeMessage createEmailForm(String email, String authCode) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        message.addRecipients(MimeMessage.RecipientType.TO, email);
        message.setSubject("집다음 회원가입 인증번호 발송");

        String msg = ""
                + "<div style=\"font-family: Arial, sans-serif; padding: 30px; background-color: #1e1e1e; color: #fff;\">"
                + "  <h1 style=\"font-size: 28px; color: white;\">이메일 인증번호 안내</h1>"
                + "  <p style=\"font-size: 16px; margin-top: 24px; line-height: 1.6;\">"
                + "    본 메일은 <strong>집다음</strong> 사이트의 회원가입을 위한 이메일 인증입니다.<br>"
                + "    아래의 <strong>[이메일 인증번호]</strong>를 입력하여 본인확인을 해주시기 바랍니다."
                + "  </p>"
                + "  <div style=\"background-color: #2a2a2a; padding: 30px; margin-top: 30px; margin-bottom: 40px; border-radius: 8px; text-align: center;\">"
                + "    <span style=\"font-size: 32px; font-weight: bold; letter-spacing: 3px; color: white;\">" + authCode + "</span>"
                + "  </div>"
                + "  <p style=\"font-size: 14px; color: #ccc;\">감사합니다.<br>집다음 담당 드림</p>"
                + "</div>";
        message.setText(msg, "utf-8", "html");

        message.setFrom(configEmail);

        return message;

    }

    public void sendVerificationCode(String toEmail) {

        Optional<UserDto> userByEmail = Optional.ofNullable(userMapper.findByEmail(toEmail));
        if (userByEmail.isPresent()) throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);

        if (redisUtil.exists(toEmail)) {
            redisUtil.delete(toEmail);
        }

        String authCode = createCode();
        redisUtil.setDataWithTTL(toEmail, authCode, 60 * 3L);

        try {
            MimeMessage emailForm = createEmailForm(toEmail, authCode);
            mailSender.send(emailForm);
        } catch (Exception e) {
            redisUtil.delete(toEmail);
            throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
        }

    }

    public void verifyEmailCode(String email, String code) {
        String codeFoundByEmail = redisUtil.getData(email);
        if (codeFoundByEmail == null) throw new BusinessException(ErrorCode.REQUEST_TIME_OUT);

        if (codeFoundByEmail.equals(code)) {
            delete(email);

            // 2. "인증 완료" 상태를 5분간 Redis에 저장 (예: "SIGNUP_VERIFIED:user@email.com" -> "TRUE")
            redisUtil.setDataWithTTL("SIGNUP_VERIFIED:" + email, "TRUE", 60 * 10L);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT_CODE);
        }



    }

    public boolean checkEmailVerified(String email) {
        String isVerified = redisUtil.getData("SIGNUP_VERIFIED:" + email);
        return "TRUE".equals(isVerified);
    }

    public void deleteVerifiedState(String email) {
        redisUtil.delete("SIGNUP_VERIFIED:" + email);
    }

    public void delete(String key) {
        redisUtil.delete(key);
    }


}
