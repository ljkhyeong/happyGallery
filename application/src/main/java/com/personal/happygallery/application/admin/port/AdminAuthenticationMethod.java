package com.personal.happygallery.application.admin.port;

/** 관리자 Bearer 세션을 발급할 때 마지막으로 확인한 인증 수단. */
public enum AdminAuthenticationMethod {
    PASSWORD,
    TOTP,
    RECOVERY_CODE
}
