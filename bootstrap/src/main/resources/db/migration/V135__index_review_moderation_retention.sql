ALTER TABLE review_moderation_actions
    ADD INDEX idx_review_moderation_created (created_at, id);
