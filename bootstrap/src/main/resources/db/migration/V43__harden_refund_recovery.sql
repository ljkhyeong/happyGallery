ALTER TABLE refunds
    MODIFY COLUMN status VARCHAR(30) NOT NULL
        COMMENT 'REQUESTED | PROCESSING | RETRYABLE | RECONCILIATION_REQUIRED | SUCCEEDED | FAILED',
    ADD COLUMN processing_at DATETIME(6) NULL AFTER status,
    ADD COLUMN processing_token VARCHAR(64) NULL AFTER processing_at,
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0 AFTER processing_token,
    ADD COLUMN next_attempt_at DATETIME(6) NULL AFTER attempt_count,
    ADD COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
        ON UPDATE CURRENT_TIMESTAMP(6) AFTER created_at,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER updated_at;

CREATE INDEX idx_refunds_status_next_attempt
    ON refunds (status, next_attempt_at, created_at);

CREATE INDEX idx_refunds_status_processing
    ON refunds (status, processing_at, created_at);
