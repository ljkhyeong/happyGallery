ALTER TABLE payment_attempt
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING | PROCESSING | RETRYABLE | APPROVED | CONFIRMED | FAILED | RECONCILIATION_REQUIRED | COMPENSATION_REQUESTED | COMPENSATION_FAILED | COMPENSATED | CANCELED',
    ADD COLUMN confirm_recovery_attempted_at DATETIME(6) NULL
        COMMENT '마지막 confirm 자동 복구 시도 시각' AFTER confirmed_at;

CREATE INDEX idx_payment_attempt_confirm_recovery
    ON payment_attempt (status, confirm_recovery_attempted_at, created_at);
