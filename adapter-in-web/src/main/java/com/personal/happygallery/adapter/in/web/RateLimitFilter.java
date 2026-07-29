package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.IpRules;
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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode.FAIL_CLOSED;
import static com.personal.happygallery.adapter.in.web.ratelimit.RateLimitFailureMode.FAIL_OPEN;
import static com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_PROVIDER_PATH;
import static com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes.SOCIAL_CALLBACK_PROVIDER_PATH;
import static com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes.SOCIAL_SIGNUP_INTENT_PROVIDER_PATH;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<RouteRule> ROUTE_RULES = List.of(
            new RouteRule(
                    "CUSTOMER_LOGIN_IP",
                    pathPattern(POST, "/api/v1/auth/login"),
                    FAIL_CLOSED,
                    IpRules::customerLogin),
            new RouteRule(
                    "CUSTOMER_SIGNUP_IP",
                    pathPattern(POST, "/api/v1/auth/signup"),
                    FAIL_CLOSED,
                    IpRules::customerSignup),
            new RouteRule(
                    "CUSTOMER_PASSWORD_RESET_IP",
                    pathPattern(POST, "/api/v1/auth/password/reset"),
                    FAIL_CLOSED,
                    IpRules::customerLogin),
            new RouteRule(
                    "CUSTOMER_REAUTHENTICATION_IP",
                    pathPattern(POST, "/api/v1/me/reauthentication/password"),
                    FAIL_CLOSED,
                    IpRules::customerLogin),
            new RouteRule(
                    "SOCIAL_LOGIN_IP",
                    pathPattern(GET, SOCIAL_CALLBACK_PROVIDER_PATH),
                    FAIL_CLOSED,
                    IpRules::socialLogin),
            new RouteRule(
                    "SOCIAL_LOGIN_INIT_IP",
                    new OrRequestMatcher(
                            pathPattern(GET, SOCIAL_AUTHORIZATION_PROVIDER_PATH),
                            pathPattern(POST, SOCIAL_SIGNUP_INTENT_PROVIDER_PATH)),
                    FAIL_CLOSED,
                    IpRules::socialLogin),
            new RouteRule(
                    "ADMIN_LOGIN_IP",
                    new OrRequestMatcher(
                            pathPattern(POST, "/api/v1/admin/auth/login"),
                            pathPattern(POST, "/api/v1/admin/auth/mfa/verify")),
                    FAIL_CLOSED,
                    IpRules::adminLogin),
            new RouteRule(
                    "ADMIN_SETUP_IP",
                    pathPattern(POST, "/api/v1/admin/setup"),
                    FAIL_CLOSED,
                    IpRules::adminSetup),
            new RouteRule(
                    "PHONE_VERIFICATION_IP",
                    pathPattern(POST, "/api/v1/bookings/phone-verifications"),
                    FAIL_CLOSED,
                    IpRules::phoneVerification),
            new RouteRule(
                    "EMAIL_VERIFICATION_IP",
                    new OrRequestMatcher(
                            pathPattern(POST, "/api/v1/me/email-verifications"),
                            pathPattern(PATCH, "/api/v1/me/email")),
                    FAIL_CLOSED,
                    IpRules::emailVerification),
            new RouteRule(
                    "PAYMENT_PREPARE_IP",
                    pathPattern(POST, "/api/v1/payments/prepare"),
                    FAIL_CLOSED,
                    IpRules::paymentPrepare),
            new RouteRule(
                    "PAYMENT_CONFIRM_IP",
                    pathPattern(POST, "/api/v1/payments/confirm"),
                    FAIL_OPEN,
                    IpRules::paymentConfirm),
            new RouteRule(
                    "GUEST_CLAIM_VERIFY_IP",
                    pathPattern(POST, "/api/v1/me/guest-claims/verify"),
                    FAIL_CLOSED,
                    IpRules::guestClaimVerify),
            new RouteRule(
                    "GUEST_RECORD_RECOVERY_IP",
                    new OrRequestMatcher(
                            pathPattern(POST, "/api/v1/guest-records/recovery"),
                            pathPattern(POST, "/api/v1/guest-records/payment-status-recovery")),
                    FAIL_CLOSED,
                    IpRules::guestRecordRecovery),
            new RouteRule(
                    "CLIENT_MONITORING_IP",
                    pathPattern(POST, "/api/v1/monitoring/client-events"),
                    FAIL_CLOSED,
                    IpRules::clientMonitoring),
            new RouteRule(
                    "ORDER_CUSTOMER_ACTION_IP",
                    new OrRequestMatcher(
                            pathPattern(DELETE, "/api/v1/orders/{id}"),
                            pathPattern(POST, "/api/v1/orders/{id}/delay-response"),
                            pathPattern(DELETE, "/api/v1/me/orders/{id}"),
                            pathPattern(POST, "/api/v1/me/orders/{id}/delay-response")),
                    FAIL_CLOSED,
                    IpRules::orderCustomerAction),
            new RouteRule(
                    "ADMIN_API_IP",
                    pathPattern("/api/v1/admin/**"),
                    FAIL_CLOSED,
                    IpRules::adminApi),
            new RouteRule(
                    "DEFAULT_API_IP",
                    pathPattern("/api/v1/**"),
                    FAIL_OPEN,
                    IpRules::defaultApi));

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
                resolved.id(), resolveClientKey(request), resolved.limit());
        if (result.isEmpty()) {
            if (resolved.failureMode() == FAIL_CLOSED) {
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
                    String.valueOf(decision.retryAfterSeconds()));
            FilterErrorResponseWriter.write(response, objectMapper, ErrorCode.TOO_MANY_REQUESTS);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private ResolvedRule resolveRule(HttpServletRequest request) {
        for (RouteRule rule : ROUTE_RULES) {
            if (rule.matcher().matches(request)) {
                return rule.resolve(properties.ip());
            }
        }
        return null;
    }

    String resolveClientKey(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    private void writeServiceUnavailable(HttpServletResponse response) throws IOException {
        response.setHeader(HttpHeaders.RETRY_AFTER, "1");
        FilterErrorResponseWriter.write(response, objectMapper, ErrorCode.SERVICE_UNAVAILABLE);
    }

    private record RouteRule(
            String id,
            RequestMatcher matcher,
            RateLimitFailureMode failureMode,
            Function<IpRules, Rule> limit
    ) {
        private ResolvedRule resolve(IpRules rules) {
            return new ResolvedRule(id, failureMode, limit.apply(rules));
        }
    }

    private record ResolvedRule(String id, RateLimitFailureMode failureMode, Rule limit) {}
}
