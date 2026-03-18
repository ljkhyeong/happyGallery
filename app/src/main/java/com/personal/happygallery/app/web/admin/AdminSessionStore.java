package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort;
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

    public AdminSessionStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Clock clock) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public String create(Long adminUserId, String username) {
        String token = UUID.randomUUID().toString();
        AdminSession session = new AdminSession(adminUserId, username, Instant.now(clock));
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(KEY_PREFIX + token, json, SESSION_TTL);
        } catch (Exception e) {
            throw new IllegalStateException("관리자 세션 직렬화 실패", e);
        }
        return token;
    }

    @Override
    public Optional<AdminSession> validate(String token) {
        String json = redisTemplate.opsForValue().get(KEY_PREFIX + token);
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, AdminSession.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void remove(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
