package com.personal.happygallery.domain.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 비회원 게스트 — guests 테이블 */
@Entity
@Table(name = "guests")
public class Guest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "phone_enc")
    private String phoneEnc;

    @Column(name = "phone_hmac", length = 64)
    private String phoneHmac;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Guest() {}

    public Guest(String name, String phone) {
        this.name = name;
        this.phone = phone;
        this.phoneVerified = false;
    }

    /** 전화번호 인증 완료 처리 */
    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    public void applyEncryption(String phoneEnc, String phoneHmac) {
        this.phoneEnc = phoneEnc;
        this.phoneHmac = phoneHmac;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPhoneEnc() { return phoneEnc; }
    public String getPhoneHmac() { return phoneHmac; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
