ALTER TABLE reviews
    DROP CHECK chk_reviews_moderation,
    ADD CONSTRAINT chk_reviews_moderation CHECK (
        (deleted_at IS NOT NULL
            AND hidden_reason IS NULL
            AND hidden_at IS NULL
            AND hidden_by_admin_id IS NULL)
        OR
        (deleted_at IS NULL
            AND status = 'PUBLISHED'
            AND hidden_reason IS NULL
            AND hidden_at IS NULL
            AND hidden_by_admin_id IS NULL)
        OR
        (deleted_at IS NULL
            AND status = 'HIDDEN'
            AND hidden_reason IS NOT NULL
            AND hidden_at IS NOT NULL
            AND hidden_by_admin_id IS NOT NULL)
    );
