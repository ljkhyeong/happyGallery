ALTER TABLE payment_attempt
    MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'PENDING'
        COMMENT 'PENDING | PROCESSING | RETRYABLE | APPROVED | CONFIRMED | FAILED | COMPENSATION_REQUESTED | COMPENSATION_FAILED | COMPENSATED | CANCELED',
    ADD COLUMN processing_at DATETIME(6) NULL AFTER status,
    ADD COLUMN fail_reason VARCHAR(500) NULL AFTER pg_ref;

ALTER TABLE refunds
    ADD COLUMN payment_attempt_id BIGINT NULL COMMENT '도메인 생성 실패 결제의 보상 환불' AFTER pass_purchase_id,
    ADD COLUMN idempotency_key VARCHAR(64) NULL COMMENT 'PG 환불 요청 멱등키' AFTER refund_transaction_key;

UPDATE refunds
SET idempotency_key = UUID()
WHERE idempotency_key IS NULL;

ALTER TABLE refunds
    MODIFY COLUMN idempotency_key VARCHAR(64) NOT NULL COMMENT 'PG 환불 요청 멱등키',
    ADD CONSTRAINT fk_refund_payment_attempt
        FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt (id),
    ADD CONSTRAINT chk_refunds_exactly_one_source
        CHECK (
            (booking_id IS NOT NULL)
            + (order_id IS NOT NULL)
            + (pass_purchase_id IS NOT NULL)
            + (payment_attempt_id IS NOT NULL) = 1
        );

CREATE UNIQUE INDEX uq_refunds_idempotency_key
    ON refunds (idempotency_key);

CREATE INDEX idx_refunds_payment_attempt
    ON refunds (payment_attempt_id);
