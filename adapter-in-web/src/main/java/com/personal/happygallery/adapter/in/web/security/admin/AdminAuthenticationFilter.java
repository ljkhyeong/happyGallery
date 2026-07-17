package com.personal.happygallery.adapter.in.web.security.admin;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public final class AdminAuthenticationFilter extends OncePerRequestFilter {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final RequestMatcher PUBLIC_ADMIN_ENDPOINTS = new OrRequestMatcher(
            endpoint(HttpMethod.POST, "/api/v1/admin/auth/login"),
            endpoint(HttpMethod.POST, "/api/v1/admin/auth/logout"),
            endpoint(HttpMethod.POST, "/api/v1/admin/setup"),
            endpoint(HttpMethod.GET, "/api/v1/admin/setup/status")
    );

    private final AuthenticationManager authenticationManager;
    private final AuthenticationFailureHandler authenticationFailureHandler;

    public AdminAuthenticationFilter(AuthenticationManager authenticationManager,
                                     AuthenticationFailureHandler authenticationFailureHandler) {
        this.authenticationManager = authenticationManager;
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return PUBLIC_ADMIN_ENDPOINTS.matches(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authenticationRequest = resolveAuthentication(request);
        if (authenticationRequest == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(authenticationRequest);
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            authenticationFailureHandler.onAuthenticationFailure(request, response, exception);
            return;
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        filterChain.doFilter(request, response);
    }

    private Authentication resolveAuthentication(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return AdminAuthenticationToken.bearerSession(authorization.substring(BEARER_PREFIX.length()));
        }

        String apiKey = request.getHeader(ADMIN_KEY_HEADER);
        if (apiKey != null) {
            return AdminAuthenticationToken.apiKey(apiKey);
        }
        return null;
    }

    private static RequestMatcher endpoint(HttpMethod method, String path) {
        return PathPatternRequestMatcher.withDefaults().matcher(method, path);
    }
}
