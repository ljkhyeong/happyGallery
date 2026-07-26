ALTER TABLE refunds
    ADD COLUMN last_recovery_at DATETIME(6) NULL AFTER next_attempt_at;

CREATE UNIQUE INDEX uq_refunds_refund_transaction_key
    ON refunds (refund_transaction_key);

CREATE INDEX idx_refunds_last_recovery
    ON refunds (last_recovery_at, created_at, id);
