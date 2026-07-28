package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/** 휴대폰 인증 코드 임시 저장 — phone_verifications 테이블 */
@Entity
@Table(name = "phone_verifications")
public class PhoneVerification {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[0-9]{6}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private String phone;

    @Transient
    private String code;

    @Column(name = "phone_hmac", nullable = false, length = 64)
    private String phoneHmac;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PhoneVerificationPurpose purpose;

    @Column(name = "code_hmac", nullable = false, length = 64)
    private String codeHmac;

    @Column(name = "code_enc", nullable = false, length = 255)
    private String codeEnc;

    @Column(nullable = false)
    private boolean delivered = false;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected PhoneVerification() {}

    public PhoneVerification(String phone,
                             String code,
                             PhoneVerificationPurpose purpose,
                             LocalDateTime expiresAt) {
        this.phone = KoreanPhoneNumber.required(phone);
        this.code = requireCode(code);
        this.purpose = java.util.Objects.requireNonNull(purpose, "purpose");
        this.expiresAt = expiresAt;
        this.delivered = false;
        this.verified = false;
    }

    public void protect(String phoneHmac, String codeHmac, String codeEnc) {
        this.phoneHmac = phoneHmac;
        this.codeHmac = codeHmac;
        this.codeEnc = codeEnc;
    }

    public void restoreProtectedFields(String phone, String code) {
        this.phone = KoreanPhoneNumber.required(phone);
        this.code = requireCode(code);
    }

    /** 인증 코드를 소모(1회 사용)한다. */
    public void markVerified() {
        this.verified = true;
    }

    /** 외부 SMS 사업자가 발송 요청을 정상 접수한 코드만 검증 대상으로 활성화한다. */
    public void markDelivered() {
        this.delivered = true;
    }

    public Long getId() { return id; }
    public String getPhone() { return phone; }
    public String getCode() { return code; }
    public String getPhoneHmac() { return phoneHmac; }
    public PhoneVerificationPurpose getPurpose() { return purpose; }
    public String getCodeHmac() { return codeHmac; }
    public String getCodeEnc() { return codeEnc; }
    public boolean isDelivered() { return delivered; }
    public boolean isVerified() { return verified; }
    public LocalDateTime getExpiresAt() { return expiresAt; }

    private static String requireCode(String code) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "인증 코드는 6자리 숫자여야 합니다.");
        }
        return code;
    }
}
