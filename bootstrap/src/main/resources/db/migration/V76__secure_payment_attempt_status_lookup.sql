ALTER TABLE payment_attempt
    ADD COLUMN owner_user_id BIGINT NULL
        COMMENT '결제 상태를 조회할 수 있는 회원 ID' AFTER fulfilled_domain_id,
    ADD COLUMN status_access_token_hash VARCHAR(64) NULL
        COMMENT '비회원 결제 상태 조회 서명 토큰 전체의 SHA-256 해시' AFTER owner_user_id;
