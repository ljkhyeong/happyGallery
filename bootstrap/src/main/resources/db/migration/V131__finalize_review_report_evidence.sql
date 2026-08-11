ALTER TABLE review_reports
    MODIFY COLUMN evidence_snapshot_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_review_reports_evidence
        FOREIGN KEY (evidence_snapshot_id)
        REFERENCES review_evidence_snapshots (id),
    ADD CONSTRAINT chk_review_report_snapshot_status CHECK (
        snapshot_status IN ('PUBLISHED', 'HIDDEN')
    ),
    DROP COLUMN snapshot_rating,
    DROP COLUMN snapshot_content,
    DROP COLUMN snapshot_edited_at;
