package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.IpRules;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.SubjectRules;
import com.personal.happygallery.adapter.in.web.ratelimit.RedisRateLimiter;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.slf4j.MDC;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.XContentTypeOptionsHeaderWriter;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

class RateLimitFilterTest {

    private static final BlindIndexer BLIND_INDEXER = new BlindIndexer(new byte[32]);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("API가 아닌 경로는 처리율 제한 없이 통과한다")
    @Test
    void passesThrough_whenPathIsNotRateLimited() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .phoneVerification(1)
                .adminApi(1)
                .build(), mockRedis());

        MockHttpServletResponse response = perform(filter, "GET", "/actuator/health");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("동일 IP에서 인증코드 발송 요청이 초과되면 429를 반환한다")
    @Test
    void returns429_whenPhoneVerificationLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .phoneVerification(1)
                .build(), mockRedis());

        perform(filter, "POST", "/api/v1/bookings/phone-verifications");
        MockHttpServletResponse secondResponse;
        try (MDC.MDCCloseable ignored = MDC.putCloseable("requestId", "rate-limit-test")) {
            secondResponse = performWithSecurityHeaders(
                    filter, "POST", "/api/v1/bookings/phone-verifications");
        }
        String secondResponseBody = secondResponse.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponse.getHeader("Retry-After")).isNotBlank();
            softly.assertThat(secondResponse.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            softly.assertThat(secondResponse.getContentType()).startsWith("application/json");
            softly.assertThat(secondResponse.getCharacterEncoding()).isEqualTo("UTF-8");
            softly.assertThat(secondResponseBody).contains(
                    "\"code\":\"TOO_MANY_REQUESTS\"",
                    "요청이 너무 많습니다.",
                    "\"requestId\":\"rate-limit-test\"");
        });
    }

    @DisplayName("동일 IP에서 관리자 요청이 초과되면 429를 반환한다")
    @Test
    void returns429_whenAdminLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .adminApi(1)
                .build(), mockRedis());

        perform(filter, "POST", "/api/v1/admin/orders/expire-pickups");
        MockHttpServletResponse secondResponse = perform(
                filter, "POST", "/api/v1/admin/orders/expire-pickups");
        String secondResponseBody = secondResponse.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponseBody).contains("\"code\":\"TOO_MANY_REQUESTS\"");
        });
    }

    @DisplayName("고객 주문 취소는 전용 처리율 제한을 적용한다")
    @Test
    void returns429_whenOrderCustomerActionLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .orderCustomerAction(1)
                .defaultApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "DELETE", "/api/v1/orders/1");
        MockHttpServletResponse secondResponse = perform(filter, "DELETE", "/api/v1/orders/1");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("X-Forwarded-For를 무시하고 servlet이 해석한 remoteAddr를 사용한다")
    @Test
    void usesRemoteAddr_regardlessOfForwardedFor() {
        RateLimitProperties properties = new TestRateLimits().build();
        RateLimitFilter filter = filter(properties, mockRedis());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/products");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(filter.resolveClientKey(request)).isEqualTo("10.0.0.1");
    }

    @DisplayName("로그인 경로는 일반 admin API보다 엄격한 rate limit이 적용된다")
    @Test
    void returns429_whenAdminLoginLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .adminLogin(1)
                .adminApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "POST", "/api/v1/admin/auth/login");
        MockHttpServletResponse secondResponse = perform(filter, "POST", "/api/v1/admin/auth/login");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("최초 관리자 setup 경로는 일반 admin API보다 별도 rate limit이 적용된다")
    @Test
    void returns429_whenAdminSetupLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .adminSetup(1)
                .adminApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "POST", "/api/v1/admin/setup");
        MockHttpServletResponse secondResponse = perform(filter, "POST", "/api/v1/admin/setup");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("구글과 네이버 로그인은 동일한 소셜 로그인 처리율 제한을 사용한다")
    @Test
    void appliesSameLimit_forGoogleAndNaverLogin() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .socialLogin(1)
                .build(), mockRedis());

        MockHttpServletResponse googleResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/google");
        MockHttpServletResponse naverResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/naver");

        assertSoftly(softly -> {
            softly.assertThat(googleResponse.getStatus()).isEqualTo(200);
            softly.assertThat(naverResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("소셜 로그인 시작은 callback과 분리된 처리율 제한을 사용한다")
    @Test
    void limitsSocialLoginInitializationSeparately() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .socialLogin(1)
                .build(), mockRedis());

        MockHttpServletResponse loginResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/google");
        MockHttpServletResponse signupIntentResponse = perform(
                filter, "POST", "/api/v1/auth/social/signup-intents/google");
        MockHttpServletResponse authorizationResponse = perform(
                filter, "GET", "/api/v1/auth/social/authorization/naver");

        assertSoftly(softly -> {
            softly.assertThat(loginResponse.getStatus()).isEqualTo(200);
            softly.assertThat(signupIntentResponse.getStatus()).isEqualTo(200);
            softly.assertThat(authorizationResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("고위험 POST 경로는 DEFAULT_API보다 구체적인 처리율 제한을 우선한다")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("highRiskPostPaths")
    void prioritizesSpecificRuleOverDefaultApi(String path) throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .defaultApi(100)
                .highRiskPosts(1)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "POST", path);
        MockHttpServletResponse secondResponse = perform(filter, "POST", path);

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(firstResponse.getHeader("X-RateLimit-Limit")).isEqualTo("1");
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
            softly.assertThat(secondResponse.getHeader("X-RateLimit-Limit")).isEqualTo("1");
        });
    }

    @DisplayName("구체 규칙이 없는 API는 DEFAULT_API 처리율 제한을 사용한다")
    @Test
    void appliesDefaultApiRule_whenNoSpecificRuleMatches() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .defaultApi(1)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "GET", "/api/v1/products");
        MockHttpServletResponse secondResponse = perform(filter, "GET", "/api/v1/products");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(firstResponse.getHeader("X-RateLimit-Limit")).isEqualTo("1");
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("결제 prepare와 confirm은 서로 독립된 처리율 제한 버킷을 사용한다")
    @Test
    void limitsPaymentPrepareAndConfirmIndependently() throws Exception {
        RateLimitFilter filter = filter(new TestRateLimits()
                .paymentPrepare(1)
                .paymentConfirm(1)
                .build(), mockRedis());

        MockHttpServletResponse firstPrepare = perform(filter, "POST", "/api/v1/payments/prepare");
        MockHttpServletResponse secondPrepare = perform(filter, "POST", "/api/v1/payments/prepare");
        MockHttpServletResponse firstConfirm = perform(filter, "POST", "/api/v1/payments/confirm");
        MockHttpServletResponse secondConfirm = perform(filter, "POST", "/api/v1/payments/confirm");

        assertSoftly(softly -> {
            softly.assertThat(firstPrepare.getStatus()).isEqualTo(200);
            softly.assertThat(secondPrepare.getStatus()).isEqualTo(429);
            softly.assertThat(firstConfirm.getStatus()).isEqualTo(200);
            softly.assertThat(secondConfirm.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("Redis DataAccessException이 발생하면 처리율 제한을 fail-open 처리한다")
    @Test
    void failsOpen_whenRedisDataAccessExceptionOccurs() throws Exception {
        RateLimitProperties properties = new TestRateLimits()
                .defaultApi(1)
                .build();
        RateLimitFilter filter = filter(properties, unavailableRedis());

        MockHttpServletResponse response = perform(filter, "GET", "/api/v1/products");

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(200);
            softly.assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        });
    }

    @DisplayName("Redis 장애 시 고위험 경로는 fail-closed로 503을 반환한다")
    @Test
    void failsClosedForHighRiskPath_whenRedisDataAccessExceptionOccurs() throws Exception {
        RateLimitProperties properties = new TestRateLimits().build();
        RateLimitFilter filter = filter(properties, unavailableRedis());

        MockHttpServletResponse response = performWithSecurityHeaders(
                filter, "POST", "/api/v1/bookings/phone-verifications");
        String responseBody = response.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(503);
            softly.assertThat(response.getHeader("Retry-After")).isEqualTo("1");
            softly.assertThat(response.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
            softly.assertThat(responseBody).contains("\"code\":\"SERVICE_UNAVAILABLE\"");
        });
    }

    @DisplayName("소수 초 처리율 제한은 Redis 밀리초 만료를 사용하고 Retry-After를 올림한다")
    @Test
    void fractionalSecondWindow_usesMillisecondsAndRoundsRetryAfterUp() throws Exception {
        StringRedisTemplate redisTemplate = mockRedis();
        RateLimitFilter filter = filter(new TestRateLimits()
                .defaultApi(1)
                .defaultApiWindow(Duration.ofMillis(1_500))
                .build(), redisTemplate);

        perform(filter, "GET", "/api/v1/products");
        MockHttpServletResponse response = perform(filter, "GET", "/api/v1/products");

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(429);
            softly.assertThat(response.getHeader("Retry-After")).isEqualTo("2");
        });
        Mockito.verify(redisTemplate, Mockito.times(2))
                .execute(any(RedisScript.class), anyList(), Mockito.eq("1500"));
    }

    private RateLimitFilter filter(RateLimitProperties properties, StringRedisTemplate redisTemplate) {
        RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate, BLIND_INDEXER, properties);
        return new RateLimitFilter(objectMapper, properties, rateLimiter);
    }

    private static MockHttpServletResponse perform(RateLimitFilter filter,
                                                   String method,
                                                   String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private static MockHttpServletResponse performWithSecurityHeaders(
            RateLimitFilter filter,
            String method,
            String path
    ) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var headerWriterFilter = new HeaderWriterFilter(
                List.of(new XContentTypeOptionsHeaderWriter()));
        headerWriterFilter.doFilter(
                request,
                response,
                (filterRequest, filterResponse) ->
                        filter.doFilter(filterRequest, filterResponse, new MockFilterChain()));
        return response;
    }

    private static Stream<String> highRiskPostPaths() {
        return Stream.of(
                "/api/v1/payments/prepare",
                "/api/v1/payments/confirm",
                "/api/v1/auth/password/reset",
                "/api/v1/me/reauthentication/password",
                "/api/v1/me/guest-claims/verify",
                "/api/v1/guest-records/recovery",
                "/api/v1/guest-records/payment-status-recovery",
                "/api/v1/monitoring/client-events"
        );
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate mockRedis() {
        ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();
        StringRedisTemplate mock = Mockito.mock(StringRedisTemplate.class);
        Mockito.when(mock.execute(any(RedisScript.class), anyList(), any(String.class)))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);
                    String key = keys.getFirst();
                    return counters.computeIfAbsent(key, ignored -> new AtomicLong()).incrementAndGet();
                });
        return mock;
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate unavailableRedis() {
        StringRedisTemplate mock = Mockito.mock(StringRedisTemplate.class);
        Mockito.when(mock.execute(any(RedisScript.class), anyList(), any(String.class)))
                .thenThrow(new DataAccessResourceFailureException("Redis unavailable"));
        return mock;
    }

    private static Rule perMinute(long capacity) {
        return new Rule(capacity, Duration.ofMinutes(1));
    }

    private static final class TestRateLimits {

        private long defaultApi = 100;
        private long phoneVerification = 10;
        private long customerLogin = 10;
        private long customerSignup = 5;
        private long adminLogin = 5;
        private long adminSetup = 5;
        private long adminApi = 120;
        private long socialLogin = 10;
        private long paymentPrepare = 100;
        private long paymentConfirm = 100;
        private long guestClaimVerify = 100;
        private long guestRecordRecovery = 100;
        private long clientMonitoring = 100;
        private long orderCustomerAction = 100;
        private Duration defaultApiWindow = Duration.ofMinutes(1);

        private TestRateLimits defaultApi(long capacity) {
            defaultApi = capacity;
            return this;
        }

        private TestRateLimits phoneVerification(long capacity) {
            phoneVerification = capacity;
            return this;
        }

        private TestRateLimits defaultApiWindow(Duration window) {
            defaultApiWindow = window;
            return this;
        }

        private TestRateLimits adminLogin(long capacity) {
            adminLogin = capacity;
            return this;
        }

        private TestRateLimits adminSetup(long capacity) {
            adminSetup = capacity;
            return this;
        }

        private TestRateLimits adminApi(long capacity) {
            adminApi = capacity;
            return this;
        }

        private TestRateLimits socialLogin(long capacity) {
            socialLogin = capacity;
            return this;
        }

        private TestRateLimits paymentPrepare(long capacity) {
            paymentPrepare = capacity;
            return this;
        }

        private TestRateLimits paymentConfirm(long capacity) {
            paymentConfirm = capacity;
            return this;
        }

        private TestRateLimits highRiskPosts(long capacity) {
            customerLogin = capacity;
            paymentPrepare = capacity;
            paymentConfirm = capacity;
            guestClaimVerify = capacity;
            guestRecordRecovery = capacity;
            clientMonitoring = capacity;
            return this;
        }

        private TestRateLimits orderCustomerAction(long capacity) {
            orderCustomerAction = capacity;
            return this;
        }

        private RateLimitProperties build() {
            return new RateLimitProperties(
                    true,
                    "test:rate",
                    new IpRules(
                            new Rule(defaultApi, defaultApiWindow),
                            perMinute(phoneVerification),
                            perMinute(100),
                            perMinute(customerLogin),
                            perMinute(customerSignup),
                            perMinute(adminLogin),
                            perMinute(adminSetup),
                            perMinute(adminApi),
                            perMinute(socialLogin),
                            perMinute(paymentPrepare),
                            perMinute(paymentConfirm),
                            perMinute(guestClaimVerify),
                            perMinute(guestRecordRecovery),
                            perMinute(clientMonitoring),
                            perMinute(orderCustomerAction)
                    ),
                    new SubjectRules(
                            perMinute(100),
                            perMinute(100),
                            perMinute(100),
                            perMinute(100),
                            perMinute(100),
                            perMinute(100),
                            perMinute(100),
                            perMinute(100),
                            perMinute(100)
                    )
            );
        }
    }
}
