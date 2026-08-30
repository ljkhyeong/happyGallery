ALTER TABLE booking_history
    ADD COLUMN admin_user_id BIGINT NULL AFTER actor;

CREATE INDEX idx_booking_history_admin_user_id_created_at
    ON booking_history (admin_user_id, created_at);
