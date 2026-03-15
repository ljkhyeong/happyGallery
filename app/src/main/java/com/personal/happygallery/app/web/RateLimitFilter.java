package com.personal.happygallery.app.web;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
import com.personal.happygallery.config.properties.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String LEGACY_ADMIN_PATH_PREFIX = "/admin/";
    private static final String VERSIONED_ADMIN_PATH_PREFIX = "/api/v1/admin/";
    private static final long BUCKET_EVICTION_SECONDS = 10 * 60; // 10분 미접근 시 제거

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
    private final Map<String, TimestampedBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper, RateLimitProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
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

        String bucketKey = resolved.rule().id() + ":" + resolveClientKey(request);
        TimestampedBucket tb = buckets.computeIfAbsent(bucketKey,
                ignored -> new TimestampedBucket(createBucket(resolved.capacity(), resolved.window())));
        tb.touch();
        ConsumptionProbe probe = tb.bucket().tryConsumeAndReturnRemaining(1);

        response.setHeader("X-RateLimit-Limit", String.valueOf(resolved.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, probe.getRemainingTokens())));

        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    /** 5분마다 오래된 bucket 제거 */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    void evictStaleBuckets() {
        Instant cutoff = Instant.now().minusSeconds(BUCKET_EVICTION_SECONDS);
        buckets.entrySet().removeIf(e -> e.getValue().lastAccessed().isBefore(cutoff));
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

    private Bucket createBucket(long capacity, Duration window) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, window));
        return Bucket.builder().addLimit(limit).build();
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

    private static final class TimestampedBucket {
        private final Bucket bucket;
        private volatile Instant lastAccessed;

        TimestampedBucket(Bucket bucket) {
            this.bucket = bucket;
            this.lastAccessed = Instant.now();
        }

        Bucket bucket() { return bucket; }
        Instant lastAccessed() { return lastAccessed; }
        void touch() { this.lastAccessed = Instant.now(); }
    }
}
