package com.personal.happygallery.app.web.admin;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminSessionStore {

    private static final long SESSION_TTL_SECONDS = 8 * 60 * 60; // 8시간

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String create(Long adminUserId, String username) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, new Session(adminUserId, username, Instant.now()));
        return token;
    }

    public Optional<Session> validate(String token) {
        Session session = sessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.createdAt().plusSeconds(SESSION_TTL_SECONDS).isBefore(Instant.now())) {
            sessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void remove(String token) {
        sessions.remove(token);
    }

    public record Session(Long adminUserId, String username, Instant createdAt) {}
}
