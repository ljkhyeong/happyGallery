package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.admin.port.out.AdminSessionPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 인메모리 관리자 세션 저장소.
 *
 * <p>{@link AdminSessionPort}의 기본 구현체로, ConcurrentHashMap 기반이다.
 * 프로덕션에서 다중 인스턴스 배포 시 Redis 등으로 교체할 수 있다.
 */
@Component
public class AdminSessionStore implements AdminSessionPort {

    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60; // 8시간

    private final Map<String, AdminSession> sessions = new ConcurrentHashMap<>();

    @Override
    public String create(Long adminUserId, String username) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new AdminSession(adminUserId, username, Instant.now()));
        return token;
    }

    @Override
    public Optional<AdminSession> validate(String token) {
        AdminSession session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.createdAt().plusSeconds(SESSION_TTL_SECONDS).isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public void remove(String token) {
        sessions.remove(token);
    }
}
