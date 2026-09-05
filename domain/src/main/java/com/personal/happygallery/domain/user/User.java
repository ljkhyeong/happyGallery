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
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Transient
    private String email;

    @Column(name = "email_enc", length = 512)
    private String emailEnc;

    @Column(name = "email_hmac", length = 64)
    private String emailHmac;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "credential_version", nullable = false)
    private long credentialVersion;

    @Version
    @Column(nullable = false)
    private long version;

    @Transient
    private String name;

    @Column(name = "name_enc", nullable = false, length = 1024)
    private String nameEnc;

    @Column(name = "name_hmac", nullable = false, length = 64)
    private String nameHmac;

    @Transient
    private String phone;

    @Column(name = "phone_enc", length = 255)
    private String phoneEnc;

    @Column(name = "phone_hmac", length = 64)
    private String phoneHmac;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    @Column(name = "default_shipping_address_enc", columnDefinition = "TEXT")
    private String defaultShippingAddressEnc;

    @Column(name = "shipping_address_version", nullable = false)
    private long shippingAddressVersion;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected User() {}

    public User(String email, String passwordHash, String name, String phone) {
        this.email = EmailAddress.required(email);
        this.passwordHash = passwordHash;
        this.name = PersonalName.required(name);
        this.phone = KoreanPhoneNumber.required(phone);
        this.phoneVerified = false;
    }

    public static User fromSocialProfile(String email, String name) {
        return new User(email, name);
    }

    private User(String email, String name) {
        this.email = EmailAddress.optional(email);
        this.passwordHash = null;
        this.name = PersonalName.required(name);
        this.phone = null;
        this.phoneVerified = false;
    }

    public void protect(String emailEnc, String emailHmac,
                        String nameEnc, String nameHmac,
                        String phoneEnc, String phoneHmac) {
        this.emailEnc = emailEnc;
        this.emailHmac = emailHmac;
        this.nameEnc = nameEnc;
        this.nameHmac = nameHmac;
        this.phoneEnc = phoneEnc;
        this.phoneHmac = phoneHmac;
    }

    public void restoreProtectedFields(String email, String name, String phone) {
        this.email = EmailAddress.optional(email);
        this.name = PersonalName.required(name);
        this.phone = KoreanPhoneNumber.optional(phone);
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getEmailEnc() { return emailEnc; }
    public String getEmailHmac() { return emailHmac; }
    public String getPasswordHash() { return passwordHash; }
    public long getCredentialVersion() { return credentialVersion; }
    public long getVersion() { return version; }
    public String getName() { return name; }
    public String getNameEnc() { return nameEnc; }
    public String getNameHmac() { return nameHmac; }
    public String getPhone() { return phone; }
    public String getPhoneEnc() { return phoneEnc; }
    public String getPhoneHmac() { return phoneHmac; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getDefaultShippingAddressEnc() { return defaultShippingAddressEnc; }
    public long getShippingAddressVersion() { return shippingAddressVersion; }

    public void changeDefaultShippingAddress(long expectedVersion, String encryptedAddress) {
        if (shippingAddressVersion != expectedVersion) {
            throw new HappyGalleryException(ErrorCode.CONFLICT, "기본 배송지가 변경되었습니다. 새로고침 후 다시 확인해 주세요.");
        }
        defaultShippingAddressEnc = encryptedAddress;
        shippingAddressVersion = Math.incrementExact(shippingAddressVersion);
    }

    public void updateLastLoginAt(LocalDateTime loginAt) {
        this.lastLoginAt = loginAt;
    }

    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    public void registerVerifiedPhone(String phone) {
        this.phone = KoreanPhoneNumber.required(phone);
        this.phoneVerified = true;
    }

    public void registerVerifiedEmail(String email) {
        if (this.email != null) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        this.email = EmailAddress.required(email);
    }

    public boolean isActive() {
        return withdrawnAt == null;
    }

    /** 거래 FK는 유지하면서 로그인 자격과 개인정보를 폐기한다. */
    public void withdraw(String anonymizedEmail, String anonymizedName, LocalDateTime withdrawnAt) {
        if (!isActive()) {
            return;
        }
        this.email = EmailAddress.required(anonymizedEmail);
        this.name = PersonalName.required(anonymizedName);
        this.phone = null;
        this.defaultShippingAddressEnc = null;
        this.passwordHash = null;
        this.phoneVerified = false;
        this.lastLoginAt = null;
        this.withdrawnAt = withdrawnAt;
        this.credentialVersion = Math.incrementExact(credentialVersion);
    }

    public boolean hasLocalPassword() {
        return passwordHash != null;
    }

    public void updatePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        this.credentialVersion = Math.incrementExact(credentialVersion);
    }

    public void upgradePasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void markAuthenticationMethodsChanged() {
        this.credentialVersion = Math.incrementExact(credentialVersion);
    }

}
