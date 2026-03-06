package com.personal.happygallery.app.web;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String LEGACY_ADMIN_PATH_PREFIX = "/admin/";
    private static final String VERSIONED_ADMIN_PATH_PREFIX = "/api/v1/admin/";

    private static final LimitRule PHONE_VERIFICATION_RULE = new LimitRule(
            "PHONE_VERIFICATION", "POST", "/bookings/phone-verifications", "/api/v1/bookings/phone-verifications");
    private static final LimitRule BOOKING_CREATE_RULE = new LimitRule(
            "BOOKING_CREATE", "POST", "/bookings/guest", "/api/v1/bookings/guest");
    private static final LimitRule PASS_PURCHASE_RULE = new LimitRule(
            "PASS_PURCHASE", "POST", "/passes/guest", "/api/v1/passes/guest");
    private static final LimitRule ADMIN_API_RULE = new LimitRule("ADMIN_API", null, null, null);

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final long phoneVerificationPerSecond;
    private final long bookingCreatePerMinute;
    private final long passPurchasePerMinute;
    private final long adminApiPerMinute;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.rate-limit.phone-verification-per-second:10}") long phoneVerificationPerSecond,
            @Value("${app.rate-limit.booking-create-per-minute:30}") long bookingCreatePerMinute,
            @Value("${app.rate-limit.pass-purchase-per-minute:20}") long passPurchasePerMinute,
            @Value("${app.rate-limit.admin-api-per-minute:120}") long adminApiPerMinute) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.phoneVerificationPerSecond = phoneVerificationPerSecond;
        this.bookingCreatePerMinute = bookingCreatePerMinute;
        this.passPurchasePerMinute = passPurchasePerMinute;
        this.adminApiPerMinute = adminApiPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled) {
            filterChain.doFilter(request, response);
            return;
        }

        ResolvedRule resolved = resolveRule(request);
        if (resolved == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String bucketKey = resolved.rule().id() + ":" + resolveClientKey(request);
        Bucket bucket = buckets.computeIfAbsent(bucketKey, ignored -> createBucket(resolved.capacity(), resolved.window()));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

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

    private ResolvedRule resolveRule(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (isAdminPath(uri)) {
            return new ResolvedRule(ADMIN_API_RULE, adminApiPerMinute, Duration.ofMinutes(1));
        }
        if (matches(request, PHONE_VERIFICATION_RULE)) {
            return new ResolvedRule(PHONE_VERIFICATION_RULE, phoneVerificationPerSecond, Duration.ofSeconds(1));
        }
        if (matches(request, BOOKING_CREATE_RULE)) {
            return new ResolvedRule(BOOKING_CREATE_RULE, bookingCreatePerMinute, Duration.ofMinutes(1));
        }
        if (matches(request, PASS_PURCHASE_RULE)) {
            return new ResolvedRule(PASS_PURCHASE_RULE, passPurchasePerMinute, Duration.ofMinutes(1));
        }
        return null;
    }

    private boolean matches(HttpServletRequest request, LimitRule rule) {
        return request.getMethod().equals(rule.method())
                && (request.getRequestURI().equals(rule.legacyPath()) || request.getRequestURI().equals(rule.versionedPath()));
    }

    private boolean isAdminPath(String uri) {
        return uri.startsWith(LEGACY_ADMIN_PATH_PREFIX) || uri.startsWith(VERSIONED_ADMIN_PATH_PREFIX);
    }

    private Bucket createBucket(long capacity, Duration window) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.greedy(capacity, window));
        return Bucket.builder().addLimit(limit).build();
    }

    private String resolveClientKey(HttpServletRequest request) {
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
