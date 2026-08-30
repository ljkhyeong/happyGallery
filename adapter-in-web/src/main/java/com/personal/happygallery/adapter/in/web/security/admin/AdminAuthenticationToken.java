package com.personal.happygallery.adapter.in.web.security.admin;

import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class AdminAuthenticationToken extends AbstractAuthenticationToken {

    private static final List<SimpleGrantedAuthority> ADMIN_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    private static final List<SimpleGrantedAuthority> MFA_ENROLLMENT_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("MFA_ENROLLMENT"));

    private final Object principal;
    private final AdminPrincipal.AuthenticationSource authenticationSource;
    private String credentials;

    private AdminAuthenticationToken(String credentials, AdminPrincipal.AuthenticationSource authenticationSource) {
        super(List.of());
        this.principal = null;
        this.credentials = credentials;
        this.authenticationSource = authenticationSource;
        super.setAuthenticated(false);
    }

    private AdminAuthenticationToken(AdminPrincipal principal) {
        super(principal.isMfaEnrollmentRequired()
                ? MFA_ENROLLMENT_AUTHORITIES
                : ADMIN_AUTHORITIES);
        this.principal = principal;
        this.credentials = null;
        this.authenticationSource = principal.authenticationSource();
        super.setAuthenticated(true);
    }

    public static AdminAuthenticationToken bearerSession(String token) {
        return new AdminAuthenticationToken(token, AdminPrincipal.AuthenticationSource.BEARER_SESSION);
    }

    public static AdminAuthenticationToken apiKey(String apiKey) {
        return new AdminAuthenticationToken(apiKey, AdminPrincipal.AuthenticationSource.API_KEY);
    }

    public static AdminAuthenticationToken authenticated(AdminPrincipal principal) {
        return new AdminAuthenticationToken(principal);
    }

    public AdminPrincipal.AuthenticationSource authenticationSource() {
        return authenticationSource;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        credentials = null;
    }

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException("인증 완료 토큰은 authenticated 팩토리로 생성해야 합니다.");
        }
        super.setAuthenticated(false);
    }
}
