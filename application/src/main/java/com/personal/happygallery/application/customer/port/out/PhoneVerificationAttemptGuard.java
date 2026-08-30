package com.personal.happygallery.application.customer.port.out;

/** 인증 코드 소비 시도에 전화번호 기준 제한을 적용한다. */
public interface PhoneVerificationAttemptGuard {

    void check(String normalizedPhone);
}
