package com.personal.happygallery.adapter.out.external.admin;

import com.personal.happygallery.application.admin.port.AdminAuthenticationMethod;
import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.support.UseCaseIT;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class AdminSessionStoreUseCaseIT {

    private static final Long ADMIN_USER_ID = 7001L;
    private static final long CREDENTIAL_VERSION = 3L;
    private static final String INDEX_KEY = "admin:sessions:" + ADMIN_USER_ID + ":" + CREDENTIAL_VERSION;

    @Autowired AdminSessionPort adminSessionPort;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired BlindIndexer blindIndexer;
    @Autowired FieldEncryptor fieldEncryptor;
    @Autowired Clock clock;
    @Autowired MeterRegistry meterRegistry;
    @Autowired ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        redisTemplate.delete(INDEX_KEY);
    }

    @DisplayName("관리자 세션과 자격 버전 인덱스를 같은 TTL로 함께 저장한다")
    @Test
    void create_storesEncryptedSessionAndIndexTogether() {
        String token = adminSessionPort.create(
                ADMIN_USER_ID,
                "operator",
                CREDENTIAL_VERSION,
                true,
                AdminAuthenticationMethod.RECOVERY_CODE);
        String tokenHash = blindIndexer.index(token);
        String sessionKey = "admin:session:" + tokenHash;

        Optional<AdminSession> validated = adminSessionPort.validate(token);
        String encryptedSession = redisTemplate.opsForValue().get(sessionKey);
        Set<String> indexedTokens = redisTemplate.opsForSet().members(INDEX_KEY);
        Long sessionTtl = redisTemplate.getExpire(sessionKey, TimeUnit.MILLISECONDS);
        Long indexTtl = redisTemplate.getExpire(INDEX_KEY, TimeUnit.MILLISECONDS);

        assertSoftly(softly -> {
            softly.assertThat(validated).contains(new AdminSession(
                    ADMIN_USER_ID,
                    "operator",
                    CREDENTIAL_VERSION,
                    true,
                    AdminAuthenticationMethod.RECOVERY_CODE,
                    Instant.now(clock)));
            softly.assertThat(encryptedSession).doesNotContain("operator", token);
            softly.assertThat(indexedTokens).containsExactly(tokenHash);
            softly.assertThat(sessionTtl).isBetween(Duration.ofHours(7).toMillis(), Duration.ofHours(8).toMillis());
            softly.assertThat(indexTtl).isBetween(Duration.ofHours(7).toMillis(), Duration.ofHours(8).toMillis());
        });

        adminSessionPort.removeAll(ADMIN_USER_ID, CREDENTIAL_VERSION);
        assertSoftly(softly -> {
            softly.assertThat(redisTemplate.hasKey(sessionKey)).isFalse();
            softly.assertThat(redisTemplate.hasKey(INDEX_KEY)).isFalse();
        });
    }

    @DisplayName("관리자 세션 인덱스 저장이 실패하면 새 세션 키를 남기지 않는다")
    @Test
    void create_indexWriteFails_removesPartiallyCreatedSession() {
        redisTemplate.opsForValue().set(INDEX_KEY, "wrong-type");
        Set<String> sessionKeysBefore = redisTemplate.keys("admin:session:*");

        assertThatThrownBy(() -> adminSessionPort.create(
                ADMIN_USER_ID,
                "operator",
                CREDENTIAL_VERSION,
                true,
                AdminAuthenticationMethod.TOTP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("관리자 세션 Redis 저장 실패");

        assertThat(redisTemplate.keys("admin:session:*")).isEqualTo(sessionKeysBefore);
    }

    @DisplayName("손상된 관리자 세션은 인증하지 않고 검증 실패를 계측한다")
    @Test
    void validate_corruptedPayload_failsClosedAndCountsFailure() {
        String token = adminSessionPort.create(
                ADMIN_USER_ID,
                "operator",
                CREDENTIAL_VERSION,
                true,
                AdminAuthenticationMethod.TOTP);
        String sessionKey = "admin:session:" + blindIndexer.index(token);
        double failuresBefore = meterRegistry.counter(
                "happygallery.admin.session.validation.failures").count();
        redisTemplate.opsForValue().set(sessionKey, "corrupted-payload");

        try {
            assertSoftly(softly -> {
                softly.assertThat(adminSessionPort.validate(token)).isEmpty();
                softly.assertThat(meterRegistry.counter(
                                "happygallery.admin.session.validation.failures").count())
                        .isEqualTo(failuresBefore + 1);
            });
        } finally {
            adminSessionPort.removeAll(ADMIN_USER_ID, CREDENTIAL_VERSION);
        }
    }

    @DisplayName("인증 수단이 없는 기존 관리자 세션 payload는 비밀번호 최소 권한으로 읽는다")
    @Test
    void validate_legacyPayloadWithoutAuthenticationMethod_defaultsToPassword() throws Exception {
        String token = "legacy-admin-session-token";
        String sessionKey = "admin:session:" + blindIndexer.index(token);
        Instant createdAt = Instant.now(clock);
        String legacyPayload = objectMapper.writeValueAsString(Map.of(
                "adminUserId", ADMIN_USER_ID,
                "username", "operator",
                "credentialVersion", CREDENTIAL_VERSION,
                "mfaEnabled", true,
                "createdAt", createdAt));
        redisTemplate.opsForValue().set(
                sessionKey,
                fieldEncryptor.encrypt(legacyPayload),
                Duration.ofMinutes(5));

        try {
            assertThat(adminSessionPort.validate(token)).contains(new AdminSession(
                    ADMIN_USER_ID,
                    "operator",
                    CREDENTIAL_VERSION,
                    true,
                    AdminAuthenticationMethod.PASSWORD,
                    createdAt));
        } finally {
            redisTemplate.delete(sessionKey);
        }
    }
}
