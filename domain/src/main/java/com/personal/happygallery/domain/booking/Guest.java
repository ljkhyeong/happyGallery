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

    @Column(name = "name_enc", nullable = false, length = 1024)
    private String nameEnc;

    @Column(name = "name_hmac", nullable = false, length = 64)
    private String nameHmac;

    @Column(name = "phone_enc", nullable = false)
    private String phoneEnc;

    @Column(name = "phone_hmac", nullable = false, length = 64)
    private String phoneHmac;

    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified = false;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Guest() {}

    public Guest(String nameEnc, String nameHmac, String phoneEnc, String phoneHmac) {
        this.nameEnc = nameEnc;
        this.nameHmac = nameHmac;
        this.phoneEnc = phoneEnc;
        this.phoneHmac = phoneHmac;
        this.phoneVerified = false;
    }

    /** 전화번호 인증 완료 처리 */
    public void markPhoneVerified() {
        this.phoneVerified = true;
    }

    public Long getId() { return id; }
    public String getNameEnc() { return nameEnc; }
    public String getNameHmac() { return nameHmac; }
    public String getPhoneEnc() { return phoneEnc; }
    public String getPhoneHmac() { return phoneHmac; }
    public boolean isPhoneVerified() { return phoneVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
