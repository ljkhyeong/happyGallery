CREATE TABLE payment_webhook_receipts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    transmission_id VARCHAR(100) NOT NULL,
    payment_attempt_id BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    received_at DATETIME(6) NOT NULL,
    processing_at DATETIME(6) NULL,
    processed_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uq_payment_webhook_receipts_transmission UNIQUE (transmission_id),
    CONSTRAINT fk_payment_webhook_receipts_attempt
        FOREIGN KEY (payment_attempt_id) REFERENCES payment_attempt (id) ON DELETE RESTRICT,
    INDEX idx_payment_webhook_receipts_pending (processed_at, processing_at, id)
);
