package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitPropertiesFixture;
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
import org.junit.jupiter.params.provider.ValueSource;
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

    @Test
    @DisplayName("단체 문의는 회원과 비회원 접수를 같은 IP 한도로 제한한다")
    void groupInquiries_sharePublicSubmissionLimit() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipGroupInquiry(5)
                .build(), mockRedis());
        for (int index = 0; index < 5; index++) {
            assertThat(perform(filter, "POST", "/api/v1/group-inquiries").getStatus()).isEqualTo(200);
        }
        assertThat(perform(filter, "POST", "/api/v1/me/group-inquiries").getStatus()).isEqualTo(429);
    }

    @DisplayName("API가 아닌 경로는 처리율 제한 없이 통과한다")
    @Test
    void passesThrough_whenPathIsNotRateLimited() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipPhoneVerification(1)
                .ipAdminApi(1)
                .build(), mockRedis());

        MockHttpServletResponse response = perform(filter, "GET", "/actuator/health");

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @DisplayName("동일 IP에서 인증코드 발송 요청이 초과되면 429를 반환한다")
    @Test
    void returns429_whenPhoneVerificationLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipPhoneVerification(1)
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
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipAdminApi(1)
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
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipOrderCustomerAction(1)
                .ipDefaultApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "DELETE", "/api/v1/orders/1");
        MockHttpServletResponse secondResponse = perform(filter, "DELETE", "/api/v1/orders/1");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("후기 신고와 이미지 업로드는 서로 독립된 IP 제한을 적용한다")
    @Test
    void limitsReviewReportAndImageUploadIndependently() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipReviewReport(1)
                .ipReviewImageUpload(1)
                .ipDefaultApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstReport = perform(
                filter, "POST", "/api/v1/me/reviews/1/reports");
        MockHttpServletResponse secondReport = perform(
                filter, "POST", "/api/v1/me/reviews/1/reports");
        MockHttpServletResponse firstImage = perform(
                filter, "POST", "/api/v1/me/reviews/1/images");
        MockHttpServletResponse secondImage = perform(
                filter, "POST", "/api/v1/me/reviews/1/images");

        assertSoftly(softly -> {
            softly.assertThat(firstReport.getStatus()).isEqualTo(200);
            softly.assertThat(secondReport.getStatus()).isEqualTo(429);
            softly.assertThat(firstImage.getStatus()).isEqualTo(200);
            softly.assertThat(secondImage.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("X-Forwarded-For를 무시하고 servlet이 해석한 remoteAddr를 사용한다")
    @Test
    void usesRemoteAddr_regardlessOfForwardedFor() {
        RateLimitProperties properties = new RateLimitPropertiesFixture().build();
        RateLimitFilter filter = filter(properties, mockRedis());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/products");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        assertThat(filter.resolveClientKey(request)).isEqualTo("10.0.0.1");
    }

    @DisplayName("로그인 경로는 일반 admin API보다 엄격한 rate limit이 적용된다")
    @Test
    void returns429_whenAdminLoginLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipAdminLogin(1)
                .ipAdminApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "POST", "/api/v1/admin/auth/login");
        MockHttpServletResponse secondResponse = perform(filter, "POST", "/api/v1/admin/auth/login");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("MFA 복구도 일반 관리자 API보다 엄격한 로그인 처리율 제한을 적용한다")
    @Test
    void returns429_whenAdminMfaRecoveryLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipAdminLogin(1)
                .ipAdminApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(
                filter, "POST", "/api/v1/admin/auth/mfa/recovery");
        MockHttpServletResponse secondResponse = perform(
                filter, "POST", "/api/v1/admin/auth/mfa/recovery");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(firstResponse.getHeader("X-RateLimit-Limit")).isEqualTo("1");
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("최초 관리자 setup 경로는 일반 admin API보다 별도 rate limit이 적용된다")
    @Test
    void returns429_whenAdminSetupLimitExceeded() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipAdminSetup(1)
                .ipAdminApi(100)
                .build(), mockRedis());

        MockHttpServletResponse firstResponse = perform(filter, "POST", "/api/v1/admin/setup");
        MockHttpServletResponse secondResponse = perform(filter, "POST", "/api/v1/admin/setup");

        assertSoftly(softly -> {
            softly.assertThat(firstResponse.getStatus()).isEqualTo(200);
            softly.assertThat(secondResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("구글과 네이버와 카카오 로그인은 동일한 소셜 로그인 처리율 제한을 사용한다")
    @Test
    void appliesSameLimit_forAllSocialLogins() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipSocialLogin(2)
                .build(), mockRedis());

        MockHttpServletResponse googleResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/google");
        MockHttpServletResponse naverResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/naver");
        MockHttpServletResponse kakaoResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/kakao");

        assertSoftly(softly -> {
            softly.assertThat(googleResponse.getStatus()).isEqualTo(200);
            softly.assertThat(naverResponse.getStatus()).isEqualTo(200);
            softly.assertThat(kakaoResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("소셜 로그인 시작은 callback과 분리된 처리율 제한을 사용한다")
    @Test
    void limitsSocialLoginInitializationSeparately() throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipSocialLogin(1)
                .build(), mockRedis());

        MockHttpServletResponse loginResponse = perform(
                filter, "GET", "/api/v1/auth/social/callback/google");
        MockHttpServletResponse signupIntentResponse = perform(
                filter, "POST", "/api/v1/auth/social/signup-intents/google");
        MockHttpServletResponse authorizationResponse = perform(
                filter, "GET", "/api/v1/auth/social/authorization/naver");
        MockHttpServletResponse completionResponse = perform(
                filter, "POST", "/api/v1/auth/social/signup-completion");

        assertSoftly(softly -> {
            softly.assertThat(loginResponse.getStatus()).isEqualTo(200);
            softly.assertThat(signupIntentResponse.getStatus()).isEqualTo(200);
            softly.assertThat(authorizationResponse.getStatus()).isEqualTo(429);
            softly.assertThat(completionResponse.getStatus()).isEqualTo(429);
        });
    }

    @DisplayName("고위험 POST 경로는 DEFAULT_API보다 구체적인 처리율 제한을 우선한다")
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("highRiskPostPaths")
    void prioritizesSpecificRuleOverDefaultApi(String path) throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipDefaultApi(100)
                .ipCustomerLogin(1)
                .ipAdminLogin(1)
                .ipPaymentPrepare(1)
                .ipPaymentConfirm(1)
                .ipGuestClaimVerify(1)
                .ipGuestRecordRecovery(1)
                .ipClientMonitoring(1)
                .ipReviewReport(1)
                .ipReviewImageUpload(1)
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
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipDefaultApi(1)
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
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipPaymentPrepare(1)
                .ipPaymentConfirm(1)
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
        RateLimitProperties properties = new RateLimitPropertiesFixture()
                .ipDefaultApi(1)
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
        RateLimitProperties properties = new RateLimitPropertiesFixture().build();
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

    @DisplayName("Redis 장애 시 후기 신고와 이미지 업로드를 fail-closed로 차단한다")
    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/me/reviews/1/reports",
            "/api/v1/me/reviews/1/images"
    })
    void failsClosedForReviewWrite_whenRedisUnavailable(String path) throws Exception {
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture().build(), unavailableRedis());

        MockHttpServletResponse response = perform(filter, "POST", path);
        String responseBody = response.getContentAsString();

        assertSoftly(softly -> {
            softly.assertThat(response.getStatus()).isEqualTo(503);
            softly.assertThat(response.getHeader("Retry-After")).isEqualTo("1");
            softly.assertThat(responseBody)
                    .contains("\"code\":\"SERVICE_UNAVAILABLE\"");
        });
    }

    @DisplayName("소수 초 처리율 제한은 Redis 밀리초 만료를 사용하고 Retry-After를 올림한다")
    @Test
    void fractionalSecondWindow_usesMillisecondsAndRoundsRetryAfterUp() throws Exception {
        StringRedisTemplate redisTemplate = mockRedis();
        RateLimitFilter filter = filter(new RateLimitPropertiesFixture()
                .ipDefaultApi(1, Duration.ofMillis(1_500))
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
                "/api/v1/admin/auth/mfa/recovery",
                "/api/v1/me/guest-claims/verify",
                "/api/v1/guest-records/recovery",
                "/api/v1/guest-records/payment-status-recovery",
                "/api/v1/monitoring/client-events",
                "/api/v1/me/reviews/1/reports",
                "/api/v1/me/reviews/1/images"
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
}
