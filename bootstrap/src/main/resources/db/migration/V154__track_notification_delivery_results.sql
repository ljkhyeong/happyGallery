ALTER TABLE notification_outbox
    ADD COLUMN provider_channel VARCHAR(10) NULL AFTER last_error,
    ADD COLUMN provider_request_id VARCHAR(100) NULL AFTER provider_channel,
    ADD COLUMN provider_recipient_seq BIGINT NULL AFTER provider_request_id,
    MODIFY COLUMN status VARCHAR(20) NOT NULL
        COMMENT 'PENDING | PROCESSING | DELIVERY_PENDING | DELIVERY_CHECKING | SENT | OBSOLETE | FAILED';

CREATE INDEX idx_notification_outbox_delivery_result
    ON notification_outbox (status, next_attempt_at, locked_at, id);

ALTER TABLE notification_log
    ADD COLUMN provider_request_id VARCHAR(100) NULL AFTER fail_reason,
    ADD COLUMN provider_recipient_seq BIGINT NULL AFTER provider_request_id;

CREATE UNIQUE INDEX uk_notification_log_provider_delivery
    ON notification_log (channel, provider_request_id, provider_recipient_seq);
