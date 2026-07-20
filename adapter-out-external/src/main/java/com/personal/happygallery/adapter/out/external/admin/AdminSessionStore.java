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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class AdminSessionStore implements AdminSessionPort {

    private static final Duration SESSION_TTL = Duration.ofHours(8);
    private static final String KEY_PREFIX = "admin:session:";
    private static final String ADMIN_INDEX_PREFIX = "admin:sessions:";
    private static final RedisScript<Long> CREATE_SESSION_SCRIPT = new DefaultRedisScript<>("""
            local setResult = redis.pcall('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
            if type(setResult) == 'table' and setResult.err then
                return setResult
            end

            local addResult = redis.pcall('SADD', KEYS[2], ARGV[2])
            if type(addResult) == 'table' and addResult.err then
                redis.call('DEL', KEYS[1])
                return addResult
            end

            local expireResult = redis.pcall('PEXPIRE', KEYS[2], ARGV[3])
            if type(expireResult) == 'table' and expireResult.err then
                redis.call('DEL', KEYS[1])
                if addResult == 1 then
                    redis.call('SREM', KEYS[2], ARGV[2])
                end
                return expireResult
            end
            return 1
            """, Long.class);

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
        String encryptedSession = serializeAndEncrypt(session);
        String sessionKey = sessionKey(tokenHash);
        String indexKey = adminIndexKey(adminUserId, credentialVersion);
        try {
            Long result = redisTemplate.execute(
                    CREATE_SESSION_SCRIPT,
                    List.of(sessionKey, indexKey),
                    encryptedSession,
                    tokenHash,
                    Long.toString(SESSION_TTL.toMillis()));
            if (result == null || result != 1L) {
                throw new IllegalStateException("관리자 세션 Redis 저장 결과를 확인할 수 없습니다.");
            }
        } catch (Exception e) {
            throw new IllegalStateException("관리자 세션 Redis 저장 실패", e);
        }
        return token;
    }

    private String serializeAndEncrypt(AdminSession session) {
        try {
            return fieldEncryptor.encrypt(objectMapper.writeValueAsString(session));
        } catch (Exception e) {
            throw new IllegalStateException("관리자 세션 데이터 생성 실패", e);
        }
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
