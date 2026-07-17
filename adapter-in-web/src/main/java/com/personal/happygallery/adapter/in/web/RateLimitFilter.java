package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher.pathPattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final RedisScript<Long> INCREMENT_SCRIPT;

    static {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText("""
                local count = redis.call('INCR', KEYS[1])
                if count == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[1])
                end
                return count
                """);
        script.setResultType(Long.class);
        INCREMENT_SCRIPT = script;
    }

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String ADMIN_PATH_PREFIX = "/api/v1/admin/";

    private static final LimitRule PHONE_VERIFICATION_RULE = new LimitRule(
            "PHONE_VERIFICATION", pathPattern(POST, "/api/v1/bookings/phone-verifications"));
    private static final LimitRule CUSTOMER_LOGIN_RULE = new LimitRule(
            "CUSTOMER_LOGIN", pathPattern(POST, "/api/v1/auth/login"));
    private static final LimitRule CUSTOMER_SIGNUP_RULE = new LimitRule(
            "CUSTOMER_SIGNUP", pathPattern(POST, "/api/v1/auth/signup"));
    private static final LimitRule ADMIN_LOGIN_RULE = new LimitRule(
            "ADMIN_LOGIN", pathPattern(POST, "/api/v1/admin/auth/login"));
    private static final LimitRule ADMIN_SETUP_RULE = new LimitRule(
            "ADMIN_SETUP", pathPattern(POST, "/api/v1/admin/setup"));
    private static final LimitRule SOCIAL_LOGIN_RULE = new LimitRule(
            "SOCIAL_LOGIN", pathPattern(POST, "/api/v1/auth/social/{provider}"));
    private static final LimitRule SOCIAL_LOGIN_INIT_RULE = new LimitRule(
            "SOCIAL_LOGIN_INIT", pathPattern(GET, "/api/v1/auth/social/{provider}/url"));
    private static final LimitRule ADMIN_API_RULE = new LimitRule(
            "ADMIN_API", request -> request.getRequestURI().startsWith(ADMIN_PATH_PREFIX));

    private final ObjectMapper objectMapper;
    private final RateLimitProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final BlindIndexer blindIndexer;

    public RateLimitFilter(ObjectMapper objectMapper,
                           RateLimitProperties properties,
                           StringRedisTemplate redisTemplate,
                           BlindIndexer blindIndexer) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.blindIndexer = blindIndexer;
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

        String bucketKey = "rate:" + resolved.rule().id() + ":"
                + blindIndexer.index(resolveClientKey(request));
        long count = increment(bucketKey, resolved.window());
        long remaining = Math.max(0, resolved.capacity() - count);

        response.setHeader("X-RateLimit-Limit", String.valueOf(resolved.capacity()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        if (count > resolved.capacity()) {
            response.setHeader("Retry-After", String.valueOf(resolved.window().toSeconds()));
            log.warn("rate limit exceeded [rule={}]", resolved.rule().id());
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private long increment(String key, Duration window) {
        Long count = redisTemplate.execute(
                INCREMENT_SCRIPT, List.of(key), String.valueOf(window.toSeconds()));
        return count == null ? 1L : count;
    }

    private ResolvedRule resolveRule(HttpServletRequest request) {
        if (matches(request, CUSTOMER_LOGIN_RULE)) {
            return new ResolvedRule(CUSTOMER_LOGIN_RULE, properties.customerLoginPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, CUSTOMER_SIGNUP_RULE)) {
            return new ResolvedRule(CUSTOMER_SIGNUP_RULE, properties.customerSignupPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, SOCIAL_LOGIN_RULE)) {
            return new ResolvedRule(SOCIAL_LOGIN_RULE, properties.socialLoginPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, SOCIAL_LOGIN_INIT_RULE)) {
            return new ResolvedRule(SOCIAL_LOGIN_INIT_RULE, properties.socialLoginPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, ADMIN_LOGIN_RULE)) {
            return new ResolvedRule(ADMIN_LOGIN_RULE, properties.adminLoginPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, ADMIN_SETUP_RULE)) {
            return new ResolvedRule(ADMIN_SETUP_RULE, properties.adminSetupPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, ADMIN_API_RULE)) {
            return new ResolvedRule(ADMIN_API_RULE, properties.adminApiPerMinute(), Duration.ofMinutes(1));
        }
        if (matches(request, PHONE_VERIFICATION_RULE)) {
            return new ResolvedRule(PHONE_VERIFICATION_RULE, properties.phoneVerificationPerSecond(), Duration.ofSeconds(1));
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

    private record LimitRule(String id, RequestMatcher matcher) {
    }

    private record ResolvedRule(LimitRule rule, long capacity, Duration window) {
    }
}
