package com.personal.happygallery.domain.booking;

/** 휴대폰 인증 코드를 사용할 수 있는 단일 업무 목적. */
public enum PhoneVerificationPurpose {
    SIGNUP,
    PASSWORD_RESET,
    MEMBER_PHONE_REGISTRATION,
    MEMBER_PHONE_CHANGE,
    GUEST_BOOKING,
    GUEST_ORDER,
    GUEST_CLAIM,
    GUEST_RECORD_RECOVERY,
    GUEST_PAYMENT_STATUS_RECOVERY
}
