package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.IpRules;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.SubjectRules;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

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

    @DisplayName("동일 회원의 8회권 환불 요청이 한도를 넘으면 429 예외를 발생시킨다")
    @Test
    void rejectsRepeatedPassRefundByUser() {
        RateLimitProperties properties = properties();
        AtomicReference<String> redisKey = new AtomicReference<>();
        SubjectRateLimitGuard guard = new SubjectRateLimitGuard(
                properties,
                new RedisRateLimiter(mockRedis(redisKey), BLIND_INDEXER, properties));

        guard.checkPassRefund(42L);

        assertThatThrownBy(() -> guard.checkPassRefund(42L))
                .isInstanceOf(RateLimitExceededException.class);
        assertThat(redisKey.get())
                .isEqualTo("test:rate:PASS_REFUND_USER:" + BLIND_INDEXER.index("42"));
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
                false,
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
                        generousLimit
                ),
                new SubjectRules(
                        new Rule(1, Duration.ofMinutes(1)),
                        new Rule(1, Duration.ofMinutes(1)),
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        generousLimit,
                        new Rule(1, Duration.ofMinutes(10))
                )
        );
    }
}
