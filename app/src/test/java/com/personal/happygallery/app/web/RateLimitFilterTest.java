package com.personal.happygallery.app.web;

import com.personal.happygallery.config.properties.RateLimitProperties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;

class RateLimitFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("제한 대상이 아닌 경로는 처리율 제한 없이 통과한다")
    @Test
    void passesThrough_whenPathIsNotRateLimited() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 1, 1, 1, 1), mockRedis());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("동일 IP에서 인증코드 발송 요청이 초과되면 429를 반환한다")
    @Test
    void returns429_whenPhoneVerificationLimitExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 1, 10, 10, 10), mockRedis());

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/bookings/phone-verifications");
        first.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/bookings/phone-verifications");
        second.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new MockFilterChain());
        String secondResponseBody = secondResponse.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
            softly.assertThat(secondResponseBody).contains("\"code\":\"TOO_MANY_REQUESTS\"");
        });
    }

    @DisplayName("레거시와 v1 관리자 경로는 동일한 처리율 제한 규칙을 사용한다")
    @Test
    void appliesSameLimit_forLegacyAndV1AdminPaths() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 10, 10, 10, 1), mockRedis());

        MockHttpServletRequest legacyPathRequest = new MockHttpServletRequest("POST", "/admin/orders/expire-pickups");
        legacyPathRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(legacyPathRequest, firstResponse, new MockFilterChain());

        MockHttpServletRequest v1PathRequest = new MockHttpServletRequest("POST", "/api/v1/admin/orders/expire-pickups");
        v1PathRequest.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(v1PathRequest, secondResponse, new MockFilterChain());
        String secondResponseBody = secondResponse.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponseBody).contains("\"code\":\"TOO_MANY_REQUESTS\"");
        });
    }

    @DisplayName("trustForwardedHeaders가 false이면 X-Forwarded-For를 무시하고 remoteAddr를 사용한다")
    @Test
    void usesRemoteAddr_whenTrustForwardedHeadersDisabled() throws Exception {
        RateLimitProperties props = properties(true, 10, 10, 10, 10, 10);
        props.setTrustForwardedHeaders(false);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, props, mockRedis());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/products");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(filter.resolveClientKey(request)).isEqualTo("10.0.0.1");
    }

    @DisplayName("trustForwardedHeaders가 true이면 X-Forwarded-For 첫 번째 IP를 사용한다")
    @Test
    void usesForwardedFor_whenTrustForwardedHeadersEnabled() throws Exception {
        RateLimitProperties props = properties(true, 10, 10, 10, 10, 10);
        props.setTrustForwardedHeaders(true);
        RateLimitFilter filter = new RateLimitFilter(objectMapper, props, mockRedis());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/products");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4, 5.6.7.8");

        assertThat(filter.resolveClientKey(request)).isEqualTo("1.2.3.4");
    }

    @DisplayName("로그인 경로는 일반 admin API보다 엄격한 rate limit이 적용된다")
    @Test
    void returns429_whenAdminLoginLimitExceeded() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(objectMapper, properties(true, 10, 10, 10, 1, 100), mockRedis());

        MockHttpServletRequest first = new MockHttpServletRequest("POST", "/api/v1/admin/auth/login");
        first.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(first, firstResponse, new MockFilterChain());

        MockHttpServletRequest second = new MockHttpServletRequest("POST", "/api/v1/admin/auth/login");
        second.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        filter.doFilter(second, secondResponse, new MockFilterChain());

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    /**
     * 인메모리 카운터로 Redis INCR 동작을 흉내 내는 mock StringRedisTemplate.
     * 테스트에서 실제 Redis 없이 rate limit 동작을 검증할 수 있게 한다.
     */
    @SuppressWarnings("unchecked")
    private static StringRedisTemplate mockRedis() {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        StringRedisTemplate mock = Mockito.mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = Mockito.mock(ValueOperations.class);
        Mockito.when(mock.opsForValue()).thenReturn(ops);
        Mockito.when(ops.increment(anyString()))
                .thenAnswer(inv -> counters.computeIfAbsent(inv.getArgument(0), k -> new AtomicLong(0)).incrementAndGet());
        Mockito.doNothing().when(mock).expire(anyString(), anyLong(), any());
        return mock;
    }

    private static RateLimitProperties properties(boolean enabled,
                                                  long phoneVerificationPerSecond,
                                                  long bookingCreatePerMinute,
                                                  long passPurchasePerMinute,
                                                  long adminApiPerMinute) {
        return properties(enabled, phoneVerificationPerSecond, bookingCreatePerMinute, passPurchasePerMinute, 5, adminApiPerMinute);
    }

    private static RateLimitProperties properties(boolean enabled,
                                                  long phoneVerificationPerSecond,
                                                  long bookingCreatePerMinute,
                                                  long passPurchasePerMinute,
                                                  long adminLoginPerMinute,
                                                  long adminApiPerMinute) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setEnabled(enabled);
        properties.setPhoneVerificationPerSecond(phoneVerificationPerSecond);
        properties.setBookingCreatePerMinute(bookingCreatePerMinute);
        properties.setPassPurchasePerMinute(passPurchasePerMinute);
        properties.setAdminLoginPerMinute(adminLoginPerMinute);
        properties.setAdminApiPerMinute(adminApiPerMinute);
        return properties;
    }
}
