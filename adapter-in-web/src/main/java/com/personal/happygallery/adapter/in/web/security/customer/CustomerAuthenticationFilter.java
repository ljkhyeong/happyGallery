package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 회원 세션의 사용자 ID를 요청 범위의 Spring Security 인증으로 변환한다.
 *
 * <p>SecurityContext를 세션에 저장하지 않고 매 요청에서 사용자의 존재를 확인한다.
 * 이 필터는 SecurityContextHolderFilter 뒤, AnonymousAuthenticationFilter 앞에 등록해야 한다.
 */
public final class CustomerAuthenticationFilter extends OncePerRequestFilter {

    public static final String CUSTOMER_USER_ID_SESSION_ATTRIBUTE = "customerUserId";

    private static final List<GrantedAuthority> CUSTOMER_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    private static final RequestMatcher CUSTOMER_AUTHENTICATION_REQUESTS = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/me"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/me/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/payments/**"),
            PathPatternRequestMatcher.withDefaults().matcher("/api/v1/monitoring/client-events")
    );

    private final CustomerAuthUseCase customerAuth;

    public CustomerAuthenticationFilter(CustomerAuthUseCase customerAuth) {
        this.customerAuth = customerAuth;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !CUSTOMER_AUTHENTICATION_REQUESTS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long userId = resolveUserId(session);
        if (userId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        customerAuth.findUser(userId).ifPresentOrElse(
                user -> authenticate(CustomerPrincipal.from(user)),
                () -> session.removeAttribute(CUSTOMER_USER_ID_SESSION_ATTRIBUTE)
        );
        filterChain.doFilter(request, response);
    }

    private Long resolveUserId(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute(CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        return userId instanceof Long id ? id : null;
    }

    private void authenticate(CustomerPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        principal,
                        null,
                        CUSTOMER_AUTHORITIES
                );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
