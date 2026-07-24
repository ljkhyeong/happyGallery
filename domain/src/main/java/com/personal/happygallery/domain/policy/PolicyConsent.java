package com.personal.happygallery.domain.policy;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "policy_consents")
public class PolicyConsent {

    public static final int MAX_POLICY_VERSION_LENGTH = 40;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "payment_attempt_id")
    private Long paymentAttemptId;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30)
    private PolicyConsentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PolicyConsentPurpose purpose;

    @Column(name = "policy_version", nullable = false, length = MAX_POLICY_VERSION_LENGTH)
    private String policyVersion;

    @Column(name = "accepted_at", nullable = false)
    private LocalDateTime acceptedAt;

    protected PolicyConsent() {}

    private PolicyConsent(Long userId,
                          Long paymentAttemptId,
                          PolicyConsentType type,
                          PolicyConsentPurpose purpose,
                          String policyVersion,
                          LocalDateTime acceptedAt) {
        if ((userId == null) == (paymentAttemptId == null)) {
            throw new IllegalArgumentException("정책 동의 주체는 회원 또는 결제 시도 중 하나여야 합니다.");
        }
        this.userId = userId;
        this.paymentAttemptId = paymentAttemptId;
        this.type = Objects.requireNonNull(type);
        this.purpose = Objects.requireNonNull(purpose);
        this.policyVersion = requireVersion(policyVersion);
        this.acceptedAt = Objects.requireNonNull(acceptedAt);
    }

    public static PolicyConsent forUser(Long userId,
                                        PolicyConsentType type,
                                        PolicyConsentPurpose purpose,
                                        String policyVersion,
                                        LocalDateTime acceptedAt) {
        return new PolicyConsent(
                Objects.requireNonNull(userId), null, type, purpose, policyVersion, acceptedAt);
    }

    public static PolicyConsent forPaymentAttempt(Long paymentAttemptId,
                                                  PolicyConsentType type,
                                                  PolicyConsentPurpose purpose,
                                                  String policyVersion,
                                                  LocalDateTime acceptedAt) {
        return new PolicyConsent(
                null, Objects.requireNonNull(paymentAttemptId), type, purpose, policyVersion, acceptedAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getPaymentAttemptId() { return paymentAttemptId; }
    public PolicyConsentType getType() { return type; }
    public PolicyConsentPurpose getPurpose() { return purpose; }
    public String getPolicyVersion() { return policyVersion; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }

    private static String requireVersion(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("정책 버전은 필수입니다.");
        }
        String trimmed = version.trim();
        if (trimmed.length() > MAX_POLICY_VERSION_LENGTH) {
            throw new IllegalArgumentException(
                    "정책 버전은 " + MAX_POLICY_VERSION_LENGTH + "자 이하여야 합니다.");
        }
        return trimmed;
    }
}
