package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.IpRules;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.SubjectRules;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

class SubjectRateLimitGuardTest {

    private static final String PHONE = "01012345678";
    private static final BlindIndexer BLIND_INDEXER = new BlindIndexer(new byte[32]);

    @DisplayName("고객 로그인 이메일은 원문 대신 HMAC Redis 키로 제한한다")
    @Test
    void limitsCustomerLoginWithoutExposingRawEmailInRedisKey() {
        RateLimitProperties properties = properties();
        AtomicReference<String> redisKey = new AtomicReference<>();
        SubjectRateLimitGuard guard = new SubjectRateLimitGuard(
                properties,
                new RedisRateLimiter(mockRedis(redisKey), BLIND_INDEXER, properties));

        guard.checkCustomerLogin(" Member@Example.COM ");

        assertThatThrownBy(() -> guard.checkCustomerLogin("member@example.com"))
                .isInstanceOf(RateLimitExceededException.class);

        assertThat(redisKey.get())
                .isEqualTo("test:rate:CUSTOMER_LOGIN_EMAIL:"
                        + BLIND_INDEXER.index("member@example.com"))
                .doesNotContain("member@example.com");
    }

    @DisplayName("동일 전화번호 요청이 초과되면 429 예외를 발생시키고 Redis 키에 원문을 남기지 않는다")
    @Test
    void rejectsRepeatedPhoneWithoutExposingRawPhoneInRedisKey() {
        RateLimitProperties properties = properties();
        AtomicReference<String> redisKey = new AtomicReference<>();
        StringRedisTemplate redisTemplate = mockRedis(redisKey);
        RedisRateLimiter rateLimiter = new RedisRateLimiter(redisTemplate, BLIND_INDEXER, properties);
        SubjectRateLimitGuard guard = new SubjectRateLimitGuard(properties, rateLimiter);

        guard.checkPhoneVerification(PHONE);

        assertThatThrownBy(() -> guard.checkPhoneVerification(PHONE))
                .isInstanceOfSatisfying(RateLimitExceededException.class, exception ->
                        assertSoftly(softly -> {
                            softly.assertThat(exception.getErrorCode().httpStatus).isEqualTo(429);
                            softly.assertThat(exception.limit()).isEqualTo(1);
                            softly.assertThat(exception.remaining()).isZero();
                        }));
        assertThat(redisKey.get())
                .isEqualTo("test:rate:PHONE_VERIFICATION_PHONE:" + BLIND_INDEXER.index(PHONE))
                .doesNotContain(PHONE);
    }

    @DisplayName("회원과 관리자의 고위험 요청은 주체별 전용 한도를 넘으면 HMAC 키로 거절한다")
    @ParameterizedTest(name = "{0}")
    @MethodSource("repeatedSubjectRequests")
    void rejectsRepeatedSubjectRequest(
            String scenario,
            String bucket,
            Consumer<SubjectRateLimitGuard> request
    ) {
        RateLimitProperties properties = properties();
        AtomicReference<String> redisKey = new AtomicReference<>();
        SubjectRateLimitGuard guard = new SubjectRateLimitGuard(
                properties,
                new RedisRateLimiter(mockRedis(redisKey), BLIND_INDEXER, properties));

        request.accept(guard);

        assertThatThrownBy(() -> request.accept(guard))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(redisKey.get())
                .isEqualTo("test:rate:" + bucket + ":" + BLIND_INDEXER.index("42"));
    }

    private static Stream<Arguments> repeatedSubjectRequests() {
        return Stream.of(
                Arguments.of(
                        "8회권 환불",
                        "PASS_REFUND_USER",
                        (Consumer<SubjectRateLimitGuard>) guard -> guard.checkPassRefund(42L)),
                Arguments.of(
                        "후기 신고",
                        "REVIEW_REPORT_USER",
                        (Consumer<SubjectRateLimitGuard>) guard -> guard.checkReviewReport(42L)),
                Arguments.of(
                        "후기 생성·수정·삭제",
                        "REVIEW_MUTATION_USER",
                        (Consumer<SubjectRateLimitGuard>) guard -> guard.checkReviewMutation(42L)),
                Arguments.of(
                        "후기 도움돼요 토글",
                        "REVIEW_HELPFUL_USER",
                        (Consumer<SubjectRateLimitGuard>) guard -> guard.checkReviewHelpful(42L)),
                Arguments.of(
                        "후기 이미지 업로드",
                        "REVIEW_IMAGE_UPLOAD_USER",
                        (Consumer<SubjectRateLimitGuard>) guard -> guard.checkReviewImageUpload(42L)),
                Arguments.of(
                        "관리자 MFA 복구",
                        "ADMIN_MFA_RECOVERY_USER",
                        (Consumer<SubjectRateLimitGuard>) guard -> guard.checkAdminMfaRecovery(42L))
        );
    }

    @DisplayName("관리자 MFA 복구 버킷을 확인할 수 없으면 fail-closed로 거절한다")
    @Test
    void rejectsAdminMfaRecoveryWhenRateLimiterUnavailable() {
        RateLimitProperties properties = properties();
        RedisRateLimiter rateLimiter = Mockito.mock(RedisRateLimiter.class);
        when(rateLimiter.tryConsume(
                "ADMIN_MFA_RECOVERY_USER",
                "42",
                properties.subject().adminMfaRecovery()))
                .thenReturn(Optional.empty());
        SubjectRateLimitGuard guard = new SubjectRateLimitGuard(properties, rateLimiter);

        assertThatThrownBy(() -> guard.checkAdminMfaRecovery(42L))
                .isInstanceOf(RateLimitUnavailableException.class);
    }

    @DisplayName("후기 신고와 이미지 업로드 제한을 확인할 수 없으면 fail-closed로 거절한다")
    @Test
    void rejectsReviewWritesWhenRateLimiterUnavailable() {
        RateLimitProperties properties = properties();
        RedisRateLimiter rateLimiter = Mockito.mock(RedisRateLimiter.class);
        when(rateLimiter.tryConsume(any(), any(), any())).thenReturn(Optional.empty());
        SubjectRateLimitGuard guard = new SubjectRateLimitGuard(properties, rateLimiter);

        assertThatThrownBy(() -> guard.checkReviewReport(42L))
                .isInstanceOf(RateLimitUnavailableException.class);
        assertThatThrownBy(() -> guard.checkReviewImageUpload(42L))
                .isInstanceOf(RateLimitUnavailableException.class);
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate mockRedis(AtomicReference<String> redisKey) {
        AtomicLong count = new AtomicLong();
        StringRedisTemplate mock = Mockito.mock(StringRedisTemplate.class);
        Mockito.when(mock.execute(any(RedisScript.class), anyList(), any(String.class)))
                .thenAnswer(invocation -> {
                    List<String> keys = invocation.getArgument(1);
                    redisKey.set(keys.getFirst());
                    return count.incrementAndGet();
                });
        return mock;
    }

    private static RateLimitProperties properties() {
        Rule generousLimit = new Rule(100, Duration.ofMinutes(1));
        return new RateLimitProperties(
                true,
                "test:rate",
                new IpRules(
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit
                ),
                new SubjectRules(
                        new Rule(1, Duration.ofMinutes(1)),
                        new Rule(1, Duration.ofMinutes(1)),
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        new Rule(1, Duration.ofMinutes(10)),
                        new Rule(1, Duration.ofMinutes(1)),
                        new Rule(1, Duration.ofMinutes(10)),
                        new Rule(1, Duration.ofMinutes(10)),
                        new Rule(1, Duration.ofMinutes(10)),
                        new Rule(1, Duration.ofMinutes(10))
                )
        );
    }
}
