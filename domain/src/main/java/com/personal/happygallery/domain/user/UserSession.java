package com.personal.happygallery.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "session_token_hash", nullable = false, unique = true, length = 64)
    private String sessionTokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected UserSession() {}

    public UserSession(Long userId, String sessionTokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.sessionTokenHash = sessionTokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getSessionTokenHash() { return sessionTokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
