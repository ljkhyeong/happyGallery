CREATE TABLE review_evidence_snapshots (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id           BIGINT       NOT NULL,
    content_revision    BIGINT       NOT NULL,
    rating              INT          NOT NULL,
    content             TEXT         NOT NULL,
    edited_at           DATETIME(6)  NULL,
    provenance          VARCHAR(20)  NOT NULL,
    images_complete     BOOLEAN      NOT NULL,
    captured_at         DATETIME(6)  NOT NULL,
    retention_until     DATETIME(6)  NULL,

    CONSTRAINT chk_review_evidence_revision CHECK (content_revision >= 1),
    CONSTRAINT chk_review_evidence_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_review_evidence_provenance CHECK (provenance IN ('LIVE', 'LEGACY_REPORT')),
    CONSTRAINT fk_review_evidence_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    INDEX idx_review_evidence_review_revision (review_id, content_revision, id),
    INDEX idx_review_evidence_retention (retention_until, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
