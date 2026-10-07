package com.personal.happygallery.application.admin.port;

import java.time.Instant;
import java.util.Objects;

/** 관리자 인증 세션 정보. */
public record AdminSession(
        Long adminUserId,
        String username,
        long credentialVersion,
        boolean mfaEnabled,
        AdminAuthenticationMethod authenticationMethod,
        Instant createdAt
) {
    public AdminSession {
        Objects.requireNonNull(authenticationMethod, "관리자 인증 수단은 필수입니다.");
    }
}
