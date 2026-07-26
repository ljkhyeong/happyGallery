package com.personal.happygallery.domain.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_mfa_challenge")
public class AdminMfaChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "token_hmac", nullable = false, unique = true, length = 64)
    private String tokenHmac;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminMfaChallenge() {}

    public AdminMfaChallenge(Long adminUserId, String tokenHmac,
                             LocalDateTime expiresAt, LocalDateTime createdAt) {
        this.adminUserId = adminUserId;
        this.tokenHmac = tokenHmac;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public boolean isUsable(LocalDateTime now) {
        return consumedAt == null && now.isBefore(expiresAt);
    }

    public void consume(LocalDateTime now) {
        consumedAt = now;
    }

    public Long getId() { return id; }
    public Long getAdminUserId() { return adminUserId; }
    public String getTokenHmac() { return tokenHmac; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getConsumedAt() { return consumedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
