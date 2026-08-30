ALTER TABLE reviews
    ADD COLUMN content_revision BIGINT NOT NULL DEFAULT 1 AFTER edited_at,
    ADD CONSTRAINT chk_reviews_content_revision CHECK (content_revision >= 1);
