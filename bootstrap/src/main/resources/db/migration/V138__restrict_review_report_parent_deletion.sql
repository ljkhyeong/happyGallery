ALTER TABLE review_reports
    DROP FOREIGN KEY fk_review_reports_review,
    ADD CONSTRAINT fk_review_reports_review_restrict
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE RESTRICT;
