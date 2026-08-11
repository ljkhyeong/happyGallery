ALTER TABLE review_reports
    ADD INDEX idx_review_reports_decided (decided_at, id);
