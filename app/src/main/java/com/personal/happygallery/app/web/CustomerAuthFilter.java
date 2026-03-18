package com.personal.happygallery.app.web;

import com.personal.happygallery.app.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
import com.personal.happygallery.config.RedisConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * 회원 인증 필터.
 *
 * <p>Spring Session이 관리하는 {@link HttpSession}에서 {@code customerUserId}를 읽고,
 * DB에서 {@link com.personal.happygallery.domain.user.User}를 로드해 request attribute에 주입한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
public class CustomerAuthFilter extends OncePerRequestFilter {

    public static final String CUSTOMER_USER_ID_ATTR = "customerUserId";
    public static final String CUSTOMER_USER_ATTR = "customerUser";
    public static final String COOKIE_NAME = RedisConfig.COOKIE_NAME;

    private static final String ME_PATH = "/api/v1/me";
    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final CustomerAuthUseCase customerAuth;
    private final ObjectMapper objectMapper;

    public CustomerAuthFilter(CustomerAuthUseCase customerAuth, ObjectMapper objectMapper) {
        this.customerAuth = customerAuth;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                    FilterChain chain) throws IOException, ServletException {
        String uri = httpRequest.getRequestURI();

        if (uri.startsWith(AUTH_PATH_PREFIX)) {
            chain.doFilter(httpRequest, httpResponse);
            return;
        }

        Long userId = resolveUserId(httpRequest);
        if (userId != null) {
            customerAuth.findUser(userId).ifPresent(u -> {
                httpRequest.setAttribute(CUSTOMER_USER_ID_ATTR, u.getId());
                httpRequest.setAttribute(CUSTOMER_USER_ATTR, u);
            });
        }

        if ((uri.equals(ME_PATH) || uri.startsWith(ME_PATH + "/"))
                && httpRequest.getAttribute(CUSTOMER_USER_ATTR) == null) {
            httpResponse.setStatus(ErrorCode.UNAUTHORIZED.httpStatus);
            httpResponse.setContentType("application/json;charset=UTF-8");
            httpResponse.getWriter().write(
                    objectMapper.writeValueAsString(
                            ErrorResponse.of(ErrorCode.UNAUTHORIZED, "로그인이 필요합니다.")));
            return;
        }

        chain.doFilter(httpRequest, httpResponse);
    }

    private Long resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object attr = session.getAttribute(CUSTOMER_USER_ID_ATTR);
        return (attr instanceof Long id) ? id : null;
    }
}
