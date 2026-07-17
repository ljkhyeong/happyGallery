package com.personal.happygallery.adapter.out.external.admin;

import com.personal.happygallery.application.admin.port.AdminSession;
import com.personal.happygallery.application.admin.port.out.AdminSessionPort;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminSessionStore implements AdminSessionPort {

    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final String KEY_PREFIX = "admin:session:";

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
    public String create(Long adminUserId, String username) {
        String token = UUID.randomUUID().toString();
        AdminSession session = new AdminSession(adminUserId, username, Instant.now(clock));
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(key(token), fieldEncryptor.encrypt(json), SESSION_TTL);
        } catch (Exception e) {
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
        redisTemplate.delete(key(token));
    }

    private String key(String token) {
        return KEY_PREFIX + blindIndexer.index(token);
    }
}
