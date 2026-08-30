package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
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
    public static final String CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE = "customerCredentialVersion";

    private static final List<GrantedAuthority> CUSTOMER_AUTHORITIES =
            List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"));
    private final CustomerAuthUseCase customerAuth;
    private final RequestMatcher authenticationEndpoints;

    public CustomerAuthenticationFilter(CustomerAuthUseCase customerAuth,
                                        RequestMatcher authenticationEndpoints) {
        this.customerAuth = customerAuth;
        this.authenticationEndpoints = authenticationEndpoints;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !authenticationEndpoints.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        SessionCredentials credentials = resolveCredentials(session);
        if (credentials == null) {
            filterChain.doFilter(request, response);
            return;
        }

        customerAuth.findUser(credentials.userId())
                .filter(user -> user.getCredentialVersion() == credentials.credentialVersion())
                .ifPresentOrElse(
                        user -> authenticate(CustomerPrincipal.from(user)),
                        session::invalidate);
        filterChain.doFilter(request, response);
    }

    private SessionCredentials resolveCredentials(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object userId = session.getAttribute(CUSTOMER_USER_ID_SESSION_ATTRIBUTE);
        Object credentialVersion = session.getAttribute(CUSTOMER_CREDENTIAL_VERSION_SESSION_ATTRIBUTE);
        if (userId instanceof Long id && credentialVersion instanceof Long version) {
            return new SessionCredentials(id, version);
        }
        if (userId != null) {
            session.invalidate();
        }
        return null;
    }

    private void authenticate(CustomerPrincipal principal) {
        var authentication =
                new PreAuthenticatedAuthenticationToken(principal, null, CUSTOMER_AUTHORITIES);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    private record SessionCredentials(Long userId, long credentialVersion) {
    }
}
