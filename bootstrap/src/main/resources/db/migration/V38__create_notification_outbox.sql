CREATE TABLE notification_outbox
(
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_type  VARCHAR(10)  NOT NULL COMMENT 'GUEST | USER',
    guest_id        BIGINT       NULL,
    user_id         BIGINT       NULL,
    event_type      VARCHAR(50)  NOT NULL,
    aggregate_type  VARCHAR(40)  NULL,
    aggregate_id    BIGINT       NULL,
    idempotency_key VARCHAR(200) NOT NULL,
    status          VARCHAR(20)  NOT NULL COMMENT 'PENDING | PROCESSING | SENT | FAILED',
    attempt_count   INT          NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    locked_at       DATETIME(6)  NULL,
    processed_at    DATETIME(6)  NULL,
    last_error      VARCHAR(500) NULL,
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE UNIQUE INDEX uk_notification_outbox_idempotency
    ON notification_outbox (idempotency_key);

CREATE INDEX idx_notification_outbox_dispatch
    ON notification_outbox (status, next_attempt_at, created_at);

CREATE INDEX idx_notification_outbox_processing
    ON notification_outbox (status, locked_at);
