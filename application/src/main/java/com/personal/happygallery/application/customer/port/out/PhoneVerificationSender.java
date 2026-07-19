package com.personal.happygallery.application.customer.port.out;

/** 휴대폰 인증 코드 전용 발송 포트. */
public interface PhoneVerificationSender {

    boolean send(String phone, String verificationCode);
}
