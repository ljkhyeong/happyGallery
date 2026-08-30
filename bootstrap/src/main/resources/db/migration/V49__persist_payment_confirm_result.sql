ALTER TABLE payment_attempt
    ADD COLUMN fulfilled_domain_id BIGINT NULL
        COMMENT 'CONFIRMED 결제로 생성된 context별 도메인 ID' AFTER payload_enc,
    ADD COLUMN fulfilled_access_token_enc MEDIUMTEXT NULL
        COMMENT '게스트 조회용 raw access token 암호문' AFTER fulfilled_domain_id;
