package com.personal.happygallery.app.web;

import com.personal.happygallery.app.web.customer.CustomerAuthService;
import com.personal.happygallery.domain.user.User;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class CustomerAuthFilter implements Filter {

    public static final String CUSTOMER_USER_ID_ATTR = "customerUserId";
    public static final String CUSTOMER_USER_ATTR = "customerUser";
    public static final String COOKIE_NAME = "HG_SESSION";

    private static final String ME_PATH = "/api/v1/me";
    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final CustomerAuthService customerAuthService;

    public CustomerAuthFilter(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = httpRequest.getRequestURI();

        // auth 경로는 필터 패스
        if (uri.startsWith(AUTH_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = extractToken(httpRequest);
        Optional<User> user = Optional.empty();

        if (token != null) {
            user = customerAuthService.validateSession(token);
            user.ifPresent(u -> {
                httpRequest.setAttribute(CUSTOMER_USER_ID_ATTR, u.getId());
                httpRequest.setAttribute(CUSTOMER_USER_ATTR, u);
            });
        }

        // /api/v1/me 경로는 인증 필수
        if (uri.equals(ME_PATH) || uri.startsWith(ME_PATH + "/")) {
            if (user.isEmpty()) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                String value = cookie.getValue();
                return (value != null && !value.isBlank()) ? value : null;
            }
        }
        return null;
    }
}
