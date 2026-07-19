package com.personal.happygallery.application.customer.port.in;

/** 전화번호 소유권을 인증 코드로 확인하고 코드를 소모한다. */
public interface PhoneOwnershipVerificationUseCase {

    void verify(String phone, String verificationCode);
}
