package com.personal.happygallery.app.web;

import com.personal.happygallery.app.customer.port.in.CustomerAuthUseCase;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 회원 인증 필터.
 *
 * <p>Spring Session이 관리하는 {@link HttpSession}에서 {@code customerUserId}를 읽고,
 * DB에서 {@link com.personal.happygallery.domain.user.User}를 로드해 request attribute에 주입한다.
 *
 * <p>Spring Session의 {@code SessionRepositoryFilter} (order = {@code Integer.MIN_VALUE + 50})보다
 * 뒤에 실행되어야 하므로 order를 {@code HIGHEST_PRECEDENCE + 60}으로 설정한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 60)
public class CustomerAuthFilter implements Filter {

    public static final String CUSTOMER_USER_ID_ATTR = "customerUserId";
    public static final String CUSTOMER_USER_ATTR = "customerUser";
    /** Spring Session이 사용하는 쿠키 이름. {@link com.personal.happygallery.config.RedisConfig} 참조. */
    public static final String COOKIE_NAME = "HG_SESSION";

    private static final String ME_PATH = "/api/v1/me";
    private static final String AUTH_PATH_PREFIX = "/api/v1/auth/";

    private final CustomerAuthUseCase customerAuth;

    public CustomerAuthFilter(CustomerAuthUseCase customerAuth) {
        this.customerAuth = customerAuth;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String uri = httpRequest.getRequestURI();

        if (uri.startsWith(AUTH_PATH_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        Long userId = resolveUserId(httpRequest);
        if (userId != null) {
            customerAuth.findUser(userId).ifPresent(u -> {
                httpRequest.setAttribute(CUSTOMER_USER_ID_ATTR, u.getId());
                httpRequest.setAttribute(CUSTOMER_USER_ATTR, u);
            });
        }

        if (uri.equals(ME_PATH) || uri.startsWith(ME_PATH + "/")) {
            if (httpRequest.getAttribute(CUSTOMER_USER_ATTR) == null) {
                httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                httpResponse.setContentType("application/json;charset=UTF-8");
                httpResponse.getWriter().write("{\"code\":\"UNAUTHORIZED\",\"message\":\"로그인이 필요합니다.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private Long resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;
        Object attr = session.getAttribute(CUSTOMER_USER_ID_ATTR);
        return (attr instanceof Long id) ? id : null;
    }
}
