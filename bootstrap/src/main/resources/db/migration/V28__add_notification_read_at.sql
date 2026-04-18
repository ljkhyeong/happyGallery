ALTER TABLE notification_log ADD COLUMN read_at DATETIME(6) NULL DEFAULT NULL AFTER sent_at;

CREATE INDEX idx_notification_log_user_read  ON notification_log (user_id, read_at);
CREATE INDEX idx_notification_log_guest_read ON notification_log (guest_id, read_at);
