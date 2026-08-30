package com.personal.happygallery.adapter.in.web.security.admin;

import com.personal.happygallery.application.admin.port.AdminAuthenticationMethod;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.security.Principal;
import java.util.Objects;

public final class AdminPrincipal implements Principal {

    public enum AuthenticationSource {
        BEARER_SESSION,
        API_KEY
    }

    private final Long adminUserId;
    private final String username;
    private final AuthenticationSource authenticationSource;
    private final boolean mfaEnrollmentRequired;
    private final AdminAuthenticationMethod authenticationMethod;

    private AdminPrincipal(
            Long adminUserId,
            String username,
            AuthenticationSource authenticationSource,
            boolean mfaEnrollmentRequired,
            AdminAuthenticationMethod authenticationMethod) {
        this.adminUserId = adminUserId;
        this.username = username;
        this.authenticationSource = authenticationSource;
        this.mfaEnrollmentRequired = mfaEnrollmentRequired;
        this.authenticationMethod = authenticationMethod;
    }

    public static AdminPrincipal bearerSession(Long adminUserId, String username) {
        return bearerSession(adminUserId, username, false);
    }

    public static AdminPrincipal bearerSession(
            Long adminUserId,
            String username,
            boolean mfaEnrollmentRequired) {
        return bearerSession(
                adminUserId,
                username,
                mfaEnrollmentRequired,
                AdminAuthenticationMethod.PASSWORD);
    }

    public static AdminPrincipal bearerSession(
            Long adminUserId,
            String username,
            boolean mfaEnrollmentRequired,
            AdminAuthenticationMethod authenticationMethod) {
        return new AdminPrincipal(
                adminUserId,
                username,
                AuthenticationSource.BEARER_SESSION,
                mfaEnrollmentRequired,
                authenticationMethod);
    }

    public static AdminPrincipal apiKey() {
        return new AdminPrincipal(
                null, null, AuthenticationSource.API_KEY, false, null);
    }

    public AuthenticationSource authenticationSource() {
        return authenticationSource;
    }

    /**
     * local API key 작업은 계정 주체가 없으므로 감사 이력 ID가 비어 있을 수 있다.
     */
    public Long auditActorId() {
        return adminUserId;
    }

    public boolean isMfaEnrollmentRequired() {
        return mfaEnrollmentRequired;
    }

    public boolean isRecoveryCodeAuthenticated() {
        return authenticationSource == AuthenticationSource.BEARER_SESSION
                && authenticationMethod == AdminAuthenticationMethod.RECOVERY_CODE;
    }

    public AdminAuthenticationMethod authenticationMethod() {
        return authenticationMethod;
    }

    public Long requireBearerAdminUserId() {
        if (authenticationSource != AuthenticationSource.BEARER_SESSION || adminUserId == null) {
            throw new HappyGalleryException(
                    ErrorCode.FORBIDDEN, "계정 기반 Bearer 관리자 세션이 필요한 작업입니다.");
        }
        return adminUserId;
    }

    @Override
    public String getName() {
        return Objects.requireNonNullElse(username, "local-api-key");
    }
}
