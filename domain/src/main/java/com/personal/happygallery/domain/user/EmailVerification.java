package com.personal.happygallery.domain.user;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(name = "email_verifications")
public class EmailVerification {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    @Transient
    private String email;

    @Transient
    private String code;

    @Column(name = "email_hmac", nullable = false, length = 64)
    private String emailHmac;

    @Column(name = "code_hmac", nullable = false, length = 64)
    private String codeHmac;

    @Column(name = "code_enc", nullable = false, length = 255)
    private String codeEnc;

    @Column(nullable = false)
    private boolean delivered;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected EmailVerification() {}

    public EmailVerification(Long userId,
                             long credentialVersion,
                             String email,
                             String code,
                             LocalDateTime expiresAt) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId must be positive");
        }
        if (credentialVersion < 0) {
            throw new IllegalArgumentException("credentialVersion must not be negative");
        }
        this.userId = userId;
        this.credentialVersion = credentialVersion;
        this.email = EmailAddress.required(email);
        this.code = requireCode(code);
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    }

    public void protect(String emailHmac, String codeHmac, String codeEnc) {
        this.emailHmac = Objects.requireNonNull(emailHmac, "emailHmac");
        this.codeHmac = Objects.requireNonNull(codeHmac, "codeHmac");
        this.codeEnc = Objects.requireNonNull(codeEnc, "codeEnc");
    }

    public void restoreProtectedFields(String email, String code) {
        this.email = EmailAddress.required(email);
        this.code = requireCode(code);
    }

    public void markDelivered() {
        this.delivered = true;
    }

    public void markVerified() {
        this.verified = true;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public long getCredentialVersion() { return credentialVersion; }
    public String getEmail() { return email; }
    public String getCode() { return code; }
    public String getEmailHmac() { return emailHmac; }
    public String getCodeHmac() { return codeHmac; }
    public String getCodeEnc() { return codeEnc; }
    public boolean isDelivered() { return delivered; }
    public boolean isVerified() { return verified; }
    public LocalDateTime getExpiresAt() { return expiresAt; }

    private static String requireCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "인증 코드는 6자리 숫자여야 합니다.");
        }
        return code;
    }
}
