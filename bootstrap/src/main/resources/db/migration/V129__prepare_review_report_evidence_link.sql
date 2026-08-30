ALTER TABLE review_reports
    DROP CHECK chk_review_report_snapshot,
    ADD COLUMN evidence_snapshot_id BIGINT NULL AFTER snapshot_status;
