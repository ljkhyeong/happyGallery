package com.personal.happygallery.adapter.out.external.admin;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

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
    @Autowired Clock clock;

    @AfterEach
    void tearDown() {
        redisTemplate.delete(INDEX_KEY);
    }

    @DisplayName("관리자 세션과 자격 버전 인덱스를 같은 TTL로 함께 저장한다")
    @Test
    void create_storesEncryptedSessionAndIndexTogether() {
        String token = adminSessionPort.create(ADMIN_USER_ID, "operator", CREDENTIAL_VERSION);
        String tokenHash = blindIndexer.index(token);
        String sessionKey = "admin:session:" + tokenHash;

        Optional<AdminSession> validated = adminSessionPort.validate(token);
        String encryptedSession = redisTemplate.opsForValue().get(sessionKey);
        Set<String> indexedTokens = redisTemplate.opsForSet().members(INDEX_KEY);
        Long sessionTtl = redisTemplate.getExpire(sessionKey, TimeUnit.MILLISECONDS);
        Long indexTtl = redisTemplate.getExpire(INDEX_KEY, TimeUnit.MILLISECONDS);

        assertSoftly(softly -> {
            softly.assertThat(validated).contains(new AdminSession(
                    ADMIN_USER_ID, "operator", CREDENTIAL_VERSION, Instant.now(clock)));
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
                ADMIN_USER_ID, "operator", CREDENTIAL_VERSION))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("관리자 세션 Redis 저장 실패");

        assertThat(redisTemplate.keys("admin:session:*")).isEqualTo(sessionKeysBefore);
    }
}
