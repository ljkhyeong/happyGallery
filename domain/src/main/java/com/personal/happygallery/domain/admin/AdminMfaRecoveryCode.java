package com.personal.happygallery.domain.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_mfa_recovery_code")
public class AdminMfaRecoveryCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id", nullable = false)
    private Long adminUserId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminMfaRecoveryCode() {}

    public AdminMfaRecoveryCode(Long adminUserId, String codeHash, LocalDateTime createdAt) {
        this.adminUserId = adminUserId;
        this.codeHash = codeHash;
        this.createdAt = createdAt;
    }

    public void use(LocalDateTime now) {
        usedAt = now;
    }

    public Long getId() { return id; }
    public Long getAdminUserId() { return adminUserId; }
    public String getCodeHash() { return codeHash; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
