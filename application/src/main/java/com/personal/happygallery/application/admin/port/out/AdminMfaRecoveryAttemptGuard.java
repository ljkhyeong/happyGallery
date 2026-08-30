package com.personal.happygallery.application.admin.port.out;

/** 복구 코드로 인증한 관리자의 MFA 초기화 시도에 관리자 ID 기준 제한을 적용한다. */
public interface AdminMfaRecoveryAttemptGuard {

    void check(Long adminUserId);
}
