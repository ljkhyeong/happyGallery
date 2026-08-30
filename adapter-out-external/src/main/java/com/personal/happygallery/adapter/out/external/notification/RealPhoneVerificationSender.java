package com.personal.happygallery.adapter.out.external.notification;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationSender;
import com.personal.happygallery.application.notification.port.out.NotificationSendResult;
import org.springframework.web.client.RestClient;

/** 운영 환경에서 인증 코드를 NHN Cloud SMS로 발송한다. */
public class RealPhoneVerificationSender implements PhoneVerificationSender {

    private static final String MESSAGE_FORMAT = "[해피갤러리] 인증번호는 %s입니다. 5분 안에 입력해주세요.";

    private final NhnSmsClient smsClient;

    public RealPhoneVerificationSender(SmsNotificationProperties properties, RestClient smsRestClient) {
        this.smsClient = new NhnSmsClient(properties, smsRestClient);
    }

    @Override
    public boolean send(String phone, String verificationCode) {
        return sendResult(phone, verificationCode).isSuccess();
    }

    NotificationSendResult sendResult(String phone, String verificationCode) {
        return smsClient.sendVerification(phone, MESSAGE_FORMAT.formatted(verificationCode));
    }
}
