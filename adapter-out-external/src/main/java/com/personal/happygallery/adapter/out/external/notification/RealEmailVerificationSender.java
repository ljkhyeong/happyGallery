package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.EmailVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** 운영 환경에서 회원 이메일 소유 확인 코드를 SMTP로 발송한다. */
public class RealEmailVerificationSender implements EmailVerificationSender {

    private static final String MESSAGE_FORMAT = """
            해피갤러리 이메일 인증번호는 %s입니다.

            5분 안에 입력해 주세요. 본인이 요청하지 않았다면 이 메일을 무시해 주세요.
            """;

    private final JavaMailSender mailSender;
    private final EmailVerificationProperties properties;

    public RealEmailVerificationSender(
            JavaMailSender mailSender,
            EmailVerificationProperties properties
    ) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public boolean send(String email, String verificationCode) {
        return sendResult(email, verificationCode).isSuccess();
    }

    NotificationSendResult sendResult(String email, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject(properties.subject());
        message.setText(MESSAGE_FORMAT.formatted(verificationCode));
        mailSender.send(message);
        return NotificationSendResult.SUCCESS;
    }
}
