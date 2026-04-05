package com.personal.happygallery.app.web;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort;
import com.personal.happygallery.app.admin.port.out.AdminSessionPort.AdminSession;
import com.personal.happygallery.domain.error.ErrorCode;

import com.personal.happygallery.config.properties.AdminProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminAuthFilter extends OncePerRequestFilter {

    public static final String ADMIN_USER_ID_ATTR = "adminUserId";
    public static final String ADMIN_USERNAME_ATTR = "adminUsername";
    public static final String ADMIN_AUTH_SOURCE_ATTR = "adminAuthSource";

    public enum AdminAuthSource {
        BEARER_SESSION,
        API_KEY
    }

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LEGACY_ADMIN_PATH_PREFIX = "/admin/";
    private static final String VERSIONED_ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final String AUTH_PATH_SUFFIX = "/auth/";

    private final AdminProperties adminProperties;
    private final AdminSessionPort sessionPort;
    private final ObjectMapper objectMapper;

    public AdminAuthFilter(AdminProperties adminProperties, AdminSessionPort sessionPort,
                           ObjectMapper objectMapper) {
        this.adminProperties = adminProperties;
        this.sessionPort = sessionPort;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                    FilterChain chain) throws IOException, ServletException {
        String uri = httpRequest.getRequestURI();

        if (isAdminPath(uri) && !isAuthPath(uri)) {
            if (!isAuthenticated(httpRequest)) {
                FilterErrorResponseWriter.write(httpResponse, objectMapper, ErrorCode.UNAUTHORIZED);
                return;
            }
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        // 1) Bearer 토큰 검증 — 세션에서 admin id 추출
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            Optional<AdminSession> session = sessionPort.validate(token);
            if (session.isPresent()) {
                request.setAttribute(ADMIN_USER_ID_ATTR, session.get().adminUserId());
                request.setAttribute(ADMIN_USERNAME_ATTR, session.get().username());
                request.setAttribute(ADMIN_AUTH_SOURCE_ATTR, AdminAuthSource.BEARER_SESSION);
                return true;
            }
        }

        // 2) X-Admin-Key 폴백 (활성화된 경우에만)
        if (adminProperties.enableApiKeyAuth()) {
            String key = request.getHeader(ADMIN_KEY_HEADER);
            if (key != null && MessageDigest.isEqual(
                    adminProperties.apiKey().getBytes(StandardCharsets.UTF_8),
                    key.getBytes(StandardCharsets.UTF_8))) {
                request.removeAttribute(ADMIN_USER_ID_ATTR);
                request.removeAttribute(ADMIN_USERNAME_ATTR);
                request.setAttribute(ADMIN_AUTH_SOURCE_ATTR, AdminAuthSource.API_KEY);
                return true;
            }
        }

        return false;
    }

    private boolean isAdminPath(String uri) {
        return uri.startsWith(LEGACY_ADMIN_PATH_PREFIX) || uri.startsWith(VERSIONED_ADMIN_PATH_PREFIX);
    }

    private boolean isAuthPath(String uri) {
        return uri.startsWith(VERSIONED_ADMIN_PATH_PREFIX + "auth/")
                || uri.startsWith(LEGACY_ADMIN_PATH_PREFIX + "auth/");
    }
}
