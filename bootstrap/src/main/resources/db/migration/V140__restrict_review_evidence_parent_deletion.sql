ALTER TABLE review_evidence_snapshots
    DROP FOREIGN KEY fk_review_evidence_review,
    ADD CONSTRAINT fk_review_evidence_review_restrict
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE RESTRICT;
