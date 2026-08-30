ALTER TABLE review_moderation_actions
    DROP FOREIGN KEY fk_review_moderation_review,
    ADD CONSTRAINT fk_review_moderation_review_restrict
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE RESTRICT;
