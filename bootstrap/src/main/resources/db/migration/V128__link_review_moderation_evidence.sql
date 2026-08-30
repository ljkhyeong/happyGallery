ALTER TABLE review_moderation_actions
    ADD COLUMN evidence_snapshot_id BIGINT NULL AFTER admin_user_id,
    ADD CONSTRAINT fk_review_moderation_evidence
        FOREIGN KEY (evidence_snapshot_id)
        REFERENCES review_evidence_snapshots (id);
