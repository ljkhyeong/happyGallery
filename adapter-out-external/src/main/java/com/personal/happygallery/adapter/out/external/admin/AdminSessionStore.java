package com.personal.happygallery.adapter.out.external.admin;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminSessionStore implements AdminSessionPort {

    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final String KEY_PREFIX = "admin:session:";
    private static final String ADMIN_INDEX_PREFIX = "admin:sessions:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final BlindIndexer blindIndexer;
    private final FieldEncryptor fieldEncryptor;

    public AdminSessionStore(StringRedisTemplate redisTemplate,
                             ObjectMapper objectMapper,
                             Clock clock,
                             BlindIndexer blindIndexer,
                             FieldEncryptor fieldEncryptor) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.blindIndexer = blindIndexer;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    public String create(Long adminUserId, String username, long credentialVersion) {
        String token = UUID.randomUUID().toString();
        String tokenHash = blindIndexer.index(token);
        AdminSession session = new AdminSession(
                adminUserId, username, credentialVersion, Instant.now(clock));
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(sessionKey(tokenHash), fieldEncryptor.encrypt(json), SESSION_TTL);
            String indexKey = adminIndexKey(adminUserId, credentialVersion);
            redisTemplate.opsForSet().add(indexKey, tokenHash);
            redisTemplate.expire(indexKey, SESSION_TTL);
        } catch (Exception e) {
            redisTemplate.delete(sessionKey(tokenHash));
            throw new IllegalStateException("관리자 세션 직렬화 실패", e);
        }
        return token;
    }

    @Override
    public Optional<AdminSession> validate(String token) {
        String encrypted = redisTemplate.opsForValue().get(key(token));
        if (encrypted == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(
                    fieldEncryptor.decrypt(encrypted), AdminSession.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void remove(String token) {
        String tokenHash = blindIndexer.index(token);
        Optional<AdminSession> session = validate(token);
        redisTemplate.delete(sessionKey(tokenHash));
        session.ifPresent(value -> redisTemplate.opsForSet()
                .remove(adminIndexKey(value.adminUserId(), value.credentialVersion()), tokenHash));
    }

    @Override
    public void removeAll(Long adminUserId, long credentialVersion) {
        String indexKey = adminIndexKey(adminUserId, credentialVersion);
        Set<String> tokenHashes = redisTemplate.opsForSet().members(indexKey);
        if (tokenHashes != null && !tokenHashes.isEmpty()) {
            List<String> sessionKeys = tokenHashes.stream()
                    .map(this::sessionKey)
                    .toList();
            redisTemplate.delete(sessionKeys);
        }
        redisTemplate.delete(indexKey);
    }

    private String key(String token) {
        return sessionKey(blindIndexer.index(token));
    }

    private String sessionKey(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }

    private String adminIndexKey(Long adminUserId, long credentialVersion) {
        return ADMIN_INDEX_PREFIX + adminUserId + ":" + credentialVersion;
    }
}
