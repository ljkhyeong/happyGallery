ALTER TABLE payment_attempt
    ADD COLUMN owner_phone_hmac VARCHAR(64) NULL
        COMMENT '비회원 결제 상태 복구용 정규화 휴대폰 HMAC' AFTER owner_user_id,
    ADD COLUMN owner_phone_hmac_key_id VARCHAR(32) NULL
        COMMENT '비회원 결제 휴대폰 HMAC 생성 키 ID' AFTER owner_phone_hmac;

CREATE INDEX idx_payment_attempt_guest_recovery
    ON payment_attempt (owner_phone_hmac, id);
