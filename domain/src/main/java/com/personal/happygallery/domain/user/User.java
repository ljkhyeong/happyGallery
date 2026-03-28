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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_enc")
    private String emailEnc;

    @Column(name = "email_hmac", length = 64)
    private String emailHmac;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id")
    private String providerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "phone_enc")
    private String phoneEnc;

    @Column(name = "phone_hmac", length = 64)
    private String phoneHmac;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {}

    public User(String email, String passwordHash, String name, String phone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.name = name;
        this.phone = phone;
        this.provider = AuthProvider.LOCAL;
        this.phoneVerified = false;
    }

    public User(String email, String name, AuthProvider provider, String providerId) {
        this.email = email;
        this.passwordHash = null;
        this.name = name;
        this.phone = "";
        this.provider = provider;
        this.providerId = providerId;
        this.phoneVerified = false;
    }

    public void applyEncryption(String emailEnc, String emailHmac,
                               String phoneEnc, String phoneHmac) {
        this.emailEnc = emailEnc;
        this.emailHmac = emailHmac;
        this.phoneEnc = phoneEnc;
        this.phoneHmac = phoneHmac;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getEmailEnc() { return emailEnc; }
    public String getEmailHmac() { return emailHmac; }
    public String getPasswordHash() { return passwordHash; }
    public AuthProvider getProvider() { return provider; }
    public String getProviderId() { return providerId; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPhoneEnc() { return phoneEnc; }
    public String getPhoneHmac() { return phoneHmac; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updateLastLoginAt(LocalDateTime loginAt) {
        this.lastLoginAt = loginAt;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    public void linkProvider(AuthProvider provider, String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }
}
