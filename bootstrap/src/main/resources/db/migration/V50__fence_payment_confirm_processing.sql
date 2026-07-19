ALTER TABLE payment_attempt
    ADD COLUMN processing_token VARCHAR(64) NULL
        COMMENT '현재 confirm 실행권 선점 토큰' AFTER processing_at;
