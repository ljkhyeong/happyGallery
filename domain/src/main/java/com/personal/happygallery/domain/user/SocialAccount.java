package com.personal.happygallery.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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

    @Transient
    private String providerId;

    @Column(name = "provider_id_hmac", nullable = false, length = 64)
    private String providerIdHmac;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected SocialAccount() {}

    public SocialAccount(Long userId, SocialProvider provider, String providerId) {
        this.userId = userId;
        this.provider = provider;
        this.providerId = requireProviderId(providerId);
    }

    public void protect(String providerIdHmac) {
        this.providerIdHmac = providerIdHmac;
    }

    public void restoreProviderId(String providerId) {
        this.providerId = requireProviderId(providerId);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public SocialProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getProviderIdHmac() { return providerIdHmac; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    private static String requireProviderId(String providerId) {
        if (providerId == null || providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
        return providerId;
    }
}
