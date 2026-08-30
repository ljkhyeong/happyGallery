package com.personal.happygallery.domain.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    @Column(name = "totp_secret_enc", length = 1024)
    private String totpSecretEnc;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(name = "last_accepted_totp_step")
    private Long lastAcceptedTotpStep;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AdminUser() {}

    public AdminUser(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public long getCredentialVersion() { return credentialVersion; }
    public String getTotpSecretEnc() { return totpSecretEnc; }
    public boolean isMfaEnabled() { return mfaEnabled; }
    public Long getLastAcceptedTotpStep() { return lastAcceptedTotpStep; }
    public boolean hasPendingMfaEnrollment() { return totpSecretEnc != null && !mfaEnabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.credentialVersion = Math.incrementExact(credentialVersion);
    }

    public void upgradePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void beginMfaEnrollment(String encryptedSecret) {
        if (mfaEnabled) {
            throw new IllegalStateException("MFA가 이미 활성화되어 있습니다.");
        }
        totpSecretEnc = encryptedSecret;
        mfaEnabled = false;
        lastAcceptedTotpStep = null;
    }

    public boolean acceptTotpStep(long timeStep) {
        if (lastAcceptedTotpStep != null && timeStep <= lastAcceptedTotpStep) {
            return false;
        }
        lastAcceptedTotpStep = timeStep;
        return true;
    }

    public long enableMfa() {
        if (mfaEnabled) {
            throw new IllegalStateException("MFA가 이미 활성화되어 있습니다.");
        }
        if (totpSecretEnc == null) {
            throw new IllegalStateException("등록 중인 MFA 비밀키가 없습니다.");
        }
        long invalidatedCredentialVersion = credentialVersion;
        mfaEnabled = true;
        credentialVersion = Math.incrementExact(credentialVersion);
        return invalidatedCredentialVersion;
    }

    public long disableMfa() {
        if (!mfaEnabled) {
            throw new IllegalStateException("MFA가 활성화되어 있지 않습니다.");
        }
        long invalidatedCredentialVersion = credentialVersion;
        totpSecretEnc = null;
        mfaEnabled = false;
        lastAcceptedTotpStep = null;
        credentialVersion = Math.incrementExact(credentialVersion);
        return invalidatedCredentialVersion;
    }
}
