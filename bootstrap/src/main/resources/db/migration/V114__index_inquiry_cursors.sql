ALTER TABLE inquiry
    DROP INDEX idx_inquiry_user_created,
    ADD INDEX idx_inquiry_user_created_id
        (user_id, created_at DESC, id DESC),
    ADD INDEX idx_inquiry_created_id
        (created_at DESC, id DESC);
