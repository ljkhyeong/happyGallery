package com.personal.happygallery.app.web;

import com.personal.happygallery.app.web.admin.AdminSessionStore;
import com.personal.happygallery.config.properties.AdminProperties;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminAuthFilter implements Filter {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String LEGACY_ADMIN_PATH_PREFIX = "/admin/";
    private static final String VERSIONED_ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final String AUTH_PATH_SUFFIX = "/auth/";

    private final AdminProperties adminProperties;
    private final AdminSessionStore sessionStore;

    public AdminAuthFilter(AdminProperties adminProperties, AdminSessionStore sessionStore) {
        this.adminProperties = adminProperties;
        this.sessionStore = sessionStore;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        if (isAdminPath(uri) && !isAuthPath(uri)) {
            if (!isAuthenticated(httpRequest)) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"관리자 인증이 필요합니다.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        // 1) Bearer 토큰 검증
        String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());
            if (sessionStore.validate(token).isPresent()) {
                return true;
            }
        }

        // 2) X-Admin-Key 폴백 (활성화된 경우에만)
        if (adminProperties.isEnableApiKeyAuth()) {
            String key = request.getHeader(ADMIN_KEY_HEADER);
            return adminProperties.getApiKey().equals(key);
        }

        return false;
    }

    private boolean isAdminPath(String uri) {
        return uri.startsWith(LEGACY_ADMIN_PATH_PREFIX) || uri.startsWith(VERSIONED_ADMIN_PATH_PREFIX);
    }

    private boolean isAuthPath(String uri) {
        return uri.contains(AUTH_PATH_SUFFIX);
    }
}
