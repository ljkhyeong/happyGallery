package com.personal.happygallery.adapter.in.web.security.admin;

import com.personal.happygallery.adapter.in.web.config.properties.AdminProperties;
import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

public final class AdminAuthenticationProvider implements AuthenticationProvider {

    private static final String INVALID_CREDENTIALS_MESSAGE = "관리자 인증 정보가 유효하지 않습니다.";

    private final AdminAuthUseCase adminAuthUseCase;
    private final AdminProperties adminProperties;

    public AdminAuthenticationProvider(AdminAuthUseCase adminAuthUseCase, AdminProperties adminProperties) {
        this.adminAuthUseCase = adminAuthUseCase;
        this.adminProperties = adminProperties;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        AdminAuthenticationToken token = (AdminAuthenticationToken) authentication;
        String credentials = (String) token.getCredentials();

        return switch (token.authenticationSource()) {
            case BEARER_SESSION -> authenticateBearerSession(credentials);
            case API_KEY -> authenticateApiKey(credentials);
        };
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return AdminAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private Authentication authenticateBearerSession(String token) {
        AdminSession session = adminAuthUseCase.validateToken(token)
                .orElseThrow(AdminAuthenticationProvider::invalidCredentials);
        boolean mfaEnrollmentRequired =
                adminProperties.requireMfaEnrollment() && !session.mfaEnabled();
        return AdminAuthenticationToken.authenticated(
                AdminPrincipal.bearerSession(
                        session.adminUserId(),
                        session.username(),
                        mfaEnrollmentRequired,
                        session.authenticationMethod())
        );
    }

    private Authentication authenticateApiKey(String apiKey) {
        String configuredApiKey = adminProperties.apiKey();
        if (!adminProperties.enableApiKeyAuth()
                || configuredApiKey.isBlank()
                || !MessageDigest.isEqual(
                        configuredApiKey.getBytes(StandardCharsets.UTF_8),
                        apiKey.getBytes(StandardCharsets.UTF_8))) {
            throw invalidCredentials();
        }
        return AdminAuthenticationToken.authenticated(AdminPrincipal.apiKey());
    }

    private static BadCredentialsException invalidCredentials() {
        return new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
    }
}
