package com.personal.happygallery.app.web;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
import com.personal.happygallery.config.properties.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Redis 기반 분산 rate limit 필터.
 *
 * <p>Redis INCR + EXPIRE 패턴으로 요청 횟수를 카운팅한다.
 * 다중 인스턴스 환경에서 인스턴스 간 카운터를 공유하므로 정확한 rate limit이 적용된다.
 *
 * <p>키 패턴: {@code rate:{RULE_ID}:{clientKey}}, TTL: 규칙의 윈도우 크기.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String LEGACY_ADMIN_PATH_PREFIX = "/admin/";
    private static final String VERSIONED_ADMIN_PATH_PREFIX = "/api/v1/admin/";

    private static final LimitRule PHONE_VERIFICATION_RULE = new LimitRule(
            "PHONE_VERIFICATION", "POST", "/bookings/phone-verifications", "/api/v1/bookings/phone-verifications");
    private static final LimitRule BOOKING_CREATE_RULE = new LimitRule(
            "BOOKING_CREATE", "POST", "/bookings/guest", "/api/v1/bookings/guest");
    private static final LimitRule PASS_PURCHASE_RULE = new LimitRule(
            "PASS_PURCHASE", "POST", "/passes/guest", "/api/v1/passes/guest");
    private static final LimitRule CUSTOMER_LOGIN_RULE = new LimitRule(
            "CUSTOMER_LOGIN", "POST", null, "/api/v1/auth/login");
    private static final LimitRule CUSTOMER_SIGNUP_RULE = new LimitRule(
            "CUSTOMER_SIGNUP", "POST", null, "/api/v1/auth/signup");
    private static final LimitRule ADMIN_LOGIN_RULE = new LimitRule(
            "ADMIN_LOGIN", "POST", "/admin/auth/login", "/api/v1/admin/auth/login");
    private static final LimitRule ADMIN_API_RULE = new LimitRule("ADMIN_API", null, null, null);

    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;

    public RateLimitFilter(ObjectMapper objectMapper,
                           RateLimitProperties properties,
                           StringRedisTemplate redisTemplate) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.isEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        ResolvedRule resolved = resolveRule(request);
        if (resolved == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = "rate:" + resolved.rule().id() + ":" + resolveClientKey(request);
        long count = increment(bucketKey, resolved.window());
        long remaining = Math.max(0, resolved.capacity() - count);

        response.setHeader("X-RateLimit-Limit", String.valueOf(resolved.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (count > resolved.capacity()) {
            long windowSeconds = resolved.window().toSeconds();
            response.setHeader("Retry-After", String.valueOf(windowSeconds));
            log.warn("rate limit exceeded [rule={} client={}]", resolved.rule().id(), bucketKey);
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Redis INCR + EXPIRE 패턴으로 카운터를 증가시킨다.
     * 키가 새로 생성된 경우(count == 1)에만 TTL을 설정해 윈도우를 시작한다.
     */
    private long increment(String key, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) count = 1L;
        if (count == 1) {
            redisTemplate.expire(key, window.toSeconds(), TimeUnit.SECONDS);
        }
        return count;
    }

    private ResolvedRule resolveRule(HttpServletRequest request) {
        if (matches(request, CUSTOMER_LOGIN_RULE)) {
            return new ResolvedRule(CUSTOMER_LOGIN_RULE, properties.getCustomerLoginPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, CUSTOMER_SIGNUP_RULE)) {
            return new ResolvedRule(CUSTOMER_SIGNUP_RULE, properties.getCustomerSignupPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, ADMIN_LOGIN_RULE)) {
            return new ResolvedRule(ADMIN_LOGIN_RULE, properties.getAdminLoginPerMinute(), Duration.ofMinutes(1));
        }
        String uri = request.getRequestURI();
        if (isAdminPath(uri)) {
            return new ResolvedRule(ADMIN_API_RULE, properties.getAdminApiPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, PHONE_VERIFICATION_RULE)) {
            return new ResolvedRule(PHONE_VERIFICATION_RULE, properties.getPhoneVerificationPerSecond(), Duration.ofSeconds(1));
        }
        if (matches(request, BOOKING_CREATE_RULE)) {
            return new ResolvedRule(BOOKING_CREATE_RULE, properties.getBookingCreatePerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, PASS_PURCHASE_RULE)) {
            return new ResolvedRule(PASS_PURCHASE_RULE, properties.getPassPurchasePerMinute(), Duration.ofMinutes(1));
        }
        return null;
    }

    private boolean matches(HttpServletRequest request, LimitRule rule) {
        if (!request.getMethod().equals(rule.method())) return false;
        String uri = request.getRequestURI();
        return (rule.legacyPath() != null && uri.equals(rule.legacyPath()))
                || (rule.versionedPath() != null && uri.equals(rule.versionedPath()));
    }

    private boolean isAdminPath(String uri) {
        return uri.startsWith(LEGACY_ADMIN_PATH_PREFIX) || uri.startsWith(VERSIONED_ADMIN_PATH_PREFIX);
    }

    String resolveClientKey(HttpServletRequest request) {
        if (!properties.isTrustForwardedHeaders()) {
            String remoteAddr = request.getRemoteAddr();
            return remoteAddr == null ? "unknown" : remoteAddr;
        }
        String forwarded = request.getHeader(X_FORWARDED_FOR);
        if (forwarded != null && !forwarded.isBlank()) {
            String[] tokens = forwarded.split(",");
            if (tokens.length > 0) {
                String ip = tokens[0].trim();
                if (!ip.isEmpty()) {
                    return ip;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(ErrorCode.TOO_MANY_REQUESTS.httpStatus);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ErrorResponse.of(ErrorCode.TOO_MANY_REQUESTS)));
    }

    private record LimitRule(String id, String method, String legacyPath, String versionedPath) {
    }

    private record ResolvedRule(LimitRule rule, long capacity, Duration window) {
    }
}
