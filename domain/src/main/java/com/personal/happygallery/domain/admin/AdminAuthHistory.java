package com.personal.happygallery.domain.admin;

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
@Table(name = "admin_auth_history")
public class AdminAuthHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_user_id")
    private Long adminUserId;

    @Column(name = "subject_hmac", length = 64)
    private String subjectHmac;

    @Column(name = "hmac_key_id", length = 32)
    private String hmacKeyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, length = 30)
    private AdminAuthOutcome outcome;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AdminAuthHistory() {}

    public AdminAuthHistory(Long adminUserId, String subjectHmac, String hmacKeyId,
                            AdminAuthOutcome outcome, LocalDateTime createdAt) {
        this.adminUserId = adminUserId;
        this.subjectHmac = subjectHmac;
        this.hmacKeyId = hmacKeyId;
        this.outcome = outcome;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public Long getAdminUserId() { return adminUserId; }
    public String getSubjectHmac() { return subjectHmac; }
    public String getHmacKeyId() { return hmacKeyId; }
    public AdminAuthOutcome getOutcome() { return outcome; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
