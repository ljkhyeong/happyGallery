package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitDecision;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode;
import com.personal.happygallery.adapter.in.web.ratelimit.RedisRateLimiter;
import com.personal.happygallery.domain.error.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode.FAIL_CLOSED;
import static com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode.FAIL_OPEN;
import static com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_PROVIDER_PATH;
import static com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes.SOCIAL_CALLBACK_PROVIDER_PATH;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private static final LimitRule PHONE_VERIFICATION_RULE = new LimitRule(
            "PHONE_VERIFICATION_IP", pathPattern(POST, "/api/v1/bookings/phone-verifications"), FAIL_CLOSED);
    private static final LimitRule CUSTOMER_LOGIN_RULE = new LimitRule(
            "CUSTOMER_LOGIN_IP", pathPattern(POST, "/api/v1/auth/login"), FAIL_CLOSED);
    private static final LimitRule CUSTOMER_SIGNUP_RULE = new LimitRule(
            "CUSTOMER_SIGNUP_IP", pathPattern(POST, "/api/v1/auth/signup"), FAIL_CLOSED);
    private static final LimitRule CUSTOMER_PASSWORD_RESET_RULE = new LimitRule(
            "CUSTOMER_PASSWORD_RESET_IP", pathPattern(POST, "/api/v1/auth/password/reset"), FAIL_CLOSED);
    private static final LimitRule ADMIN_LOGIN_RULE = new LimitRule(
            "ADMIN_LOGIN_IP",
            new OrRequestMatcher(
                    pathPattern(POST, "/api/v1/admin/auth/login"),
                    pathPattern(POST, "/api/v1/admin/auth/mfa/verify")),
            FAIL_CLOSED);
    private static final LimitRule ADMIN_SETUP_RULE = new LimitRule(
            "ADMIN_SETUP_IP", pathPattern(POST, "/api/v1/admin/setup"), FAIL_CLOSED);
    private static final LimitRule SOCIAL_LOGIN_RULE = new LimitRule(
            "SOCIAL_LOGIN_IP", pathPattern(GET, SOCIAL_CALLBACK_PROVIDER_PATH), FAIL_CLOSED);
    private static final LimitRule SOCIAL_LOGIN_INIT_RULE = new LimitRule(
            "SOCIAL_LOGIN_INIT_IP",
            pathPattern(GET, SOCIAL_AUTHORIZATION_PROVIDER_PATH), FAIL_CLOSED);
    private static final LimitRule PAYMENT_PREPARE_RULE = new LimitRule(
            "PAYMENT_PREPARE_IP", pathPattern(POST, "/api/v1/payments/prepare"), FAIL_CLOSED);
    private static final LimitRule PAYMENT_CONFIRM_RULE = new LimitRule(
            "PAYMENT_CONFIRM_IP", pathPattern(POST, "/api/v1/payments/confirm"), FAIL_OPEN);
    private static final LimitRule PRODUCT_QNA_VERIFY_RULE = new LimitRule(
            "PRODUCT_QNA_VERIFY_IP", pathPattern(POST, "/api/v1/products/{productId}/qna/{id}/verify"), FAIL_CLOSED);
    private static final LimitRule GUEST_CLAIM_VERIFY_RULE = new LimitRule(
            "GUEST_CLAIM_VERIFY_IP", pathPattern(POST, "/api/v1/me/guest-claims/verify"), FAIL_CLOSED);
    private static final LimitRule GUEST_RECORD_RECOVERY_RULE = new LimitRule(
            "GUEST_RECORD_RECOVERY_IP",
            new OrRequestMatcher(
                    pathPattern(POST, "/api/v1/guest-records/recovery"),
                    pathPattern(POST, "/api/v1/guest-records/payment-status-recovery")),
            FAIL_CLOSED);
    private static final LimitRule CLIENT_MONITORING_RULE = new LimitRule(
            "CLIENT_MONITORING_IP", pathPattern(POST, "/api/v1/monitoring/client-events"), FAIL_CLOSED);
    private static final LimitRule ORDER_CUSTOMER_ACTION_RULE = new LimitRule(
            "ORDER_CUSTOMER_ACTION_IP",
            new OrRequestMatcher(
                    pathPattern(DELETE, "/api/v1/orders/{id}"),
                    pathPattern(POST, "/api/v1/orders/{id}/delay-response"),
                    pathPattern(DELETE, "/api/v1/me/orders/{id}"),
                    pathPattern(POST, "/api/v1/me/orders/{id}/delay-response")),
            FAIL_CLOSED);
    private static final LimitRule ADMIN_API_RULE = new LimitRule(
            "ADMIN_API_IP", pathPattern("/api/v1/admin/**"), FAIL_CLOSED);
    private static final LimitRule DEFAULT_API_RULE = new LimitRule(
            "DEFAULT_API_IP", pathPattern("/api/v1/**"), FAIL_OPEN);

    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;
    private final RedisRateLimiter rateLimiter;

    public RateLimitFilter(ObjectMapper objectMapper,
                           RateLimitProperties properties,
                           RedisRateLimiter rateLimiter) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.enabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        ResolvedRule resolved = resolveRule(request);
        if (resolved == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<RateLimitDecision> result = rateLimiter.tryConsume(
                resolved.rule().id(), resolveClientKey(request), resolved.limit());
        if (result.isEmpty()) {
            if (resolved.rule().failureMode() == FAIL_CLOSED) {
                writeServiceUnavailable(response);
                return;
            }
            filterChain.doFilter(request, response);
            return;
        }
        RateLimitDecision decision = result.get();

        response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));

        if (decision.rejected()) {
            response.setHeader(HttpHeaders.RETRY_AFTER,
                    String.valueOf(Math.max(1, decision.window().toSeconds())));
            FilterErrorResponseWriter.write(response, objectMapper, ErrorCode.TOO_MANY_REQUESTS);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private ResolvedRule resolveRule(HttpServletRequest request) {
        if (matches(request, CUSTOMER_LOGIN_RULE)) {
            return new ResolvedRule(CUSTOMER_LOGIN_RULE, properties.ip().customerLogin());
        }
        if (matches(request, CUSTOMER_SIGNUP_RULE)) {
            return new ResolvedRule(CUSTOMER_SIGNUP_RULE, properties.ip().customerSignup());
        }
        if (matches(request, CUSTOMER_PASSWORD_RESET_RULE)) {
            return new ResolvedRule(CUSTOMER_PASSWORD_RESET_RULE, properties.ip().customerLogin());
        }
        if (matches(request, SOCIAL_LOGIN_RULE)) {
            return new ResolvedRule(SOCIAL_LOGIN_RULE, properties.ip().socialLogin());
        }
        if (matches(request, SOCIAL_LOGIN_INIT_RULE)) {
            return new ResolvedRule(SOCIAL_LOGIN_INIT_RULE, properties.ip().socialLogin());
        }
        if (matches(request, ADMIN_LOGIN_RULE)) {
            return new ResolvedRule(ADMIN_LOGIN_RULE, properties.ip().adminLogin());
        }
        if (matches(request, ADMIN_SETUP_RULE)) {
            return new ResolvedRule(ADMIN_SETUP_RULE, properties.ip().adminSetup());
        }
        if (matches(request, PHONE_VERIFICATION_RULE)) {
            return new ResolvedRule(PHONE_VERIFICATION_RULE, properties.ip().phoneVerification());
        }
        if (matches(request, PAYMENT_PREPARE_RULE)) {
            return new ResolvedRule(PAYMENT_PREPARE_RULE, properties.ip().paymentPrepare());
        }
        if (matches(request, PAYMENT_CONFIRM_RULE)) {
            return new ResolvedRule(PAYMENT_CONFIRM_RULE, properties.ip().paymentConfirm());
        }
        if (matches(request, PRODUCT_QNA_VERIFY_RULE)) {
            return new ResolvedRule(PRODUCT_QNA_VERIFY_RULE, properties.ip().productQnaVerify());
        }
        if (matches(request, GUEST_CLAIM_VERIFY_RULE)) {
            return new ResolvedRule(GUEST_CLAIM_VERIFY_RULE, properties.ip().guestClaimVerify());
        }
        if (matches(request, GUEST_RECORD_RECOVERY_RULE)) {
            return new ResolvedRule(GUEST_RECORD_RECOVERY_RULE, properties.ip().guestRecordRecovery());
        }
        if (matches(request, CLIENT_MONITORING_RULE)) {
            return new ResolvedRule(CLIENT_MONITORING_RULE, properties.ip().clientMonitoring());
        }
        if (matches(request, ORDER_CUSTOMER_ACTION_RULE)) {
            return new ResolvedRule(
                    ORDER_CUSTOMER_ACTION_RULE, properties.ip().orderCustomerAction());
        }
        if (matches(request, ADMIN_API_RULE)) {
            return new ResolvedRule(ADMIN_API_RULE, properties.ip().adminApi());
        }
        if (matches(request, DEFAULT_API_RULE)) {
            return new ResolvedRule(DEFAULT_API_RULE, properties.ip().defaultApi());
        }
        return null;
    }

    private boolean matches(HttpServletRequest request, LimitRule rule) {
        return rule.matcher().matches(request);
    }

    String resolveClientKey(HttpServletRequest request) {
        if (!properties.trustForwardedHeaders()) {
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr == null ? "unknown" : remoteAddr;
        }
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (StringUtils.hasText(forwarded)) {
            String ip = forwarded.split(",", 2)[0].trim();
            if (StringUtils.hasText(ip)) {
                return ip;
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
        FilterErrorResponseWriter.write(response, objectMapper, ErrorCode.SERVICE_UNAVAILABLE);
    }

    private record LimitRule(String id, RequestMatcher matcher, RateLimitFailureMode failureMode) {
    }

    private record ResolvedRule(LimitRule rule, Rule limit) {
    }
}
