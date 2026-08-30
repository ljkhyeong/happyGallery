ALTER TABLE notification_outbox
    ADD COLUMN processing_token VARCHAR(64) NULL AFTER locked_at,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 AFTER created_at;
