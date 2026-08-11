ALTER TABLE reviews
    DROP CHECK chk_reviews_moderation,
    ADD CONSTRAINT chk_reviews_moderation CHECK (
        deleted_at IS NOT NULL
        OR
        (status = 'PUBLISHED'
            AND hidden_reason IS NULL
            AND hidden_at IS NULL
            AND hidden_by_admin_id IS NULL)
        OR
        (status = 'HIDDEN'
            AND hidden_reason IS NOT NULL
            AND hidden_at IS NOT NULL
            AND hidden_by_admin_id IS NOT NULL)
    );
