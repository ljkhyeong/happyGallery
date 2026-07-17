package com.personal.happygallery.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_social_accounts")
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SocialProvider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SocialAccount() {}

    public SocialAccount(Long userId, SocialProvider provider, String providerId) {
        this.userId = userId;
        this.provider = provider;
        this.providerId = providerId;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public SocialProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
