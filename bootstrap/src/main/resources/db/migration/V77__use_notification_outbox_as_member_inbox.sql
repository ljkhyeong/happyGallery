ALTER TABLE notification_outbox
    ADD COLUMN read_at DATETIME(6) NULL AFTER processed_at;

UPDATE notification_outbox
SET read_at = processed_at
WHERE status = 'SENT';

DROP INDEX idx_notification_log_user_read ON notification_log;
DROP INDEX idx_notification_log_guest_read ON notification_log;
DROP INDEX idx_notification_log_user_sent ON notification_log;
DROP INDEX idx_notification_log_guest_sent ON notification_log;

ALTER TABLE notification_log
    DROP COLUMN read_at;

CREATE INDEX idx_notification_outbox_user_inbox
    ON notification_outbox (user_id, status, processed_at DESC, id DESC);

CREATE INDEX idx_notification_outbox_guest_inbox
    ON notification_outbox (guest_id, status, processed_at DESC, id DESC);

CREATE INDEX idx_notification_outbox_retention
    ON notification_outbox (status, processed_at, id);

CREATE INDEX idx_notification_log_retention
    ON notification_log (sent_at, id);
