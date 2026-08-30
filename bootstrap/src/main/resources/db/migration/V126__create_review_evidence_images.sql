CREATE TABLE review_evidence_snapshot_images (
    snapshot_id BIGINT       NOT NULL,
    sort_order  INT          NOT NULL,
    image_url   VARCHAR(512) NOT NULL,

    PRIMARY KEY (snapshot_id, sort_order),
    CONSTRAINT chk_review_evidence_image_order CHECK (sort_order BETWEEN 0 AND 4),
    CONSTRAINT fk_review_evidence_image_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES review_evidence_snapshots (id) ON DELETE CASCADE,
    INDEX idx_review_evidence_image_url (image_url)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
