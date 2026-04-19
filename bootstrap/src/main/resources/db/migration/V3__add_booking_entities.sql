-- ==========================================================
-- V3: 게스트 예약(§5.2) 지원
-- - phone_verifications: 휴대폰 인증 코드 임시 저장
-- - bookings.access_token: 비회원 예약 조회용 토큰
-- ==========================================================

CREATE TABLE phone_verifications
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone      VARCHAR(20)  NOT NULL,
    code       VARCHAR(6)   NOT NULL,
    verified   BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at DATETIME(6)  NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_pv_phone (phone)
);

ALTER TABLE bookings
    ADD COLUMN access_token VARCHAR(64) NULL COMMENT '비회원 예약 조회용 토큰',
    ADD UNIQUE INDEX uq_bookings_access_token (access_token);
