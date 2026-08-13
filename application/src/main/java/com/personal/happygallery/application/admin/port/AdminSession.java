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
        // 배포 전 생성된 Redis 세션 payload에는 인증 수단이 없으므로 최소 권한으로 해석한다.
        authenticationMethod = Objects.requireNonNullElse(
                authenticationMethod, AdminAuthenticationMethod.PASSWORD);
    }
}
