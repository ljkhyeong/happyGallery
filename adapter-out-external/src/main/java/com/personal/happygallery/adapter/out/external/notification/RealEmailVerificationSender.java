package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

/** 운영 환경에서 회원 이메일 소유 확인 코드를 SMTP로 발송한다. */
public class RealEmailVerificationSender implements EmailVerificationTransport {

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
    public NotificationSendResult sendResult(String email, String verificationCode) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(email);
        message.setSubject(properties.subject());
        message.setText(EmailVerificationMessage.body(verificationCode));
        mailSender.send(message);
        return NotificationSendResult.SUCCESS;
    }
}
