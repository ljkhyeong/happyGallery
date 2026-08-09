ALTER TABLE reviews
    DROP INDEX uq_reviews_order_item,
    DROP INDEX uq_reviews_booking,
    DROP INDEX idx_reviews_product_public,
    DROP INDEX idx_reviews_class_public,
    DROP INDEX idx_reviews_user_created,
    DROP INDEX idx_reviews_admin_status_created,
    DROP INDEX idx_reviews_created,
    MODIFY COLUMN rating INT NULL,
    MODIFY COLUMN content TEXT NULL,
    ADD COLUMN deleted_at DATETIME(6) NULL AFTER hidden_by_admin_id,
    ADD COLUMN recreation_blocked BOOLEAN NOT NULL DEFAULT FALSE AFTER deleted_at,
    ADD COLUMN edited_at DATETIME(6) NULL AFTER recreation_blocked,
    ADD COLUMN reply_content TEXT NULL AFTER edited_at,
    ADD COLUMN reply_admin_id BIGINT NULL AFTER reply_content,
    ADD COLUMN reply_created_at DATETIME(6) NULL AFTER reply_admin_id,
    ADD COLUMN reply_edited_at DATETIME(6) NULL AFTER reply_created_at,
    ADD COLUMN reserved_order_item_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted_at IS NULL OR recreation_blocked = TRUE THEN order_item_id
                ELSE NULL
            END
        ) STORED,
    ADD COLUMN reserved_booking_id BIGINT
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted_at IS NULL OR recreation_blocked = TRUE THEN booking_id
                ELSE NULL
            END
        ) STORED,
    ADD CONSTRAINT fk_reviews_reply_admin
        FOREIGN KEY (reply_admin_id) REFERENCES admin_user (id),
    ADD CONSTRAINT chk_reviews_deleted_content CHECK (
        (deleted_at IS NULL AND rating IS NOT NULL AND content IS NOT NULL)
        OR
        (deleted_at IS NOT NULL AND rating IS NULL AND content IS NULL)
    ),
    ADD CONSTRAINT chk_reviews_reply CHECK (
        (reply_content IS NULL
            AND reply_admin_id IS NULL
            AND reply_created_at IS NULL
            AND reply_edited_at IS NULL)
        OR
        (reply_content IS NOT NULL
            AND reply_admin_id IS NOT NULL
            AND reply_created_at IS NOT NULL)
    ),
    ADD UNIQUE INDEX uq_reviews_reserved_order_item (reserved_order_item_id),
    ADD UNIQUE INDEX uq_reviews_reserved_booking (reserved_booking_id),
    ADD INDEX idx_reviews_order_item (order_item_id, product_id),
    ADD INDEX idx_reviews_booking (booking_id, booking_class_id),
    ADD INDEX idx_reviews_product_public
        (product_id, status, deleted_at, created_at, id),
    ADD INDEX idx_reviews_product_rating_public
        (product_id, status, deleted_at, rating, created_at, id),
    ADD INDEX idx_reviews_class_public
        (booking_class_id, status, deleted_at, created_at, id),
    ADD INDEX idx_reviews_class_rating_public
        (booking_class_id, status, deleted_at, rating, created_at, id),
    ADD INDEX idx_reviews_user_created
        (user_id, deleted_at, created_at, id),
    ADD INDEX idx_reviews_admin_status_created
        (deleted_at, status, created_at, id),
    ADD INDEX idx_reviews_created
        (deleted_at, created_at, id);

UPDATE reviews
SET recreation_blocked = TRUE
WHERE status = 'HIDDEN';

CREATE TABLE review_moderation_actions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id           BIGINT       NOT NULL,
    action              VARCHAR(15)  NOT NULL,
    previous_status     VARCHAR(10)  NOT NULL,
    new_status          VARCHAR(10)  NOT NULL,
    reason              VARCHAR(500) NULL,
    admin_user_id       BIGINT       NOT NULL,
    created_at          DATETIME(6)  NOT NULL,

    CONSTRAINT chk_review_moderation_action CHECK (
        (action = 'HIDE'
            AND previous_status = 'PUBLISHED'
            AND new_status = 'HIDDEN'
            AND reason IS NOT NULL)
        OR
        (action = 'REPUBLISH'
            AND previous_status = 'HIDDEN'
            AND new_status = 'PUBLISHED'
            AND reason IS NULL)
    ),
    CONSTRAINT fk_review_moderation_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_moderation_admin
        FOREIGN KEY (admin_user_id) REFERENCES admin_user (id),
    INDEX idx_review_moderation_review_created (review_id, created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review_reports (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id             BIGINT       NOT NULL,
    reporter_user_id      BIGINT       NOT NULL,
    reason                VARCHAR(30)  NOT NULL,
    detail                VARCHAR(1000) NULL,
    snapshot_rating       INT          NOT NULL,
    snapshot_content      TEXT         NOT NULL,
    snapshot_status       VARCHAR(10)  NOT NULL,
    snapshot_edited_at    DATETIME(6)  NULL,
    status                VARCHAR(10)  NOT NULL DEFAULT 'PENDING',
    decision_note         VARCHAR(1000) NULL,
    decided_by_admin_id   BIGINT       NULL,
    decided_at            DATETIME(6)  NULL,
    created_at            DATETIME(6)  NOT NULL,

    CONSTRAINT uq_review_reports_review_reporter
        UNIQUE (review_id, reporter_user_id),
    CONSTRAINT chk_review_report_reason CHECK (
        reason IN ('SPAM', 'ABUSIVE', 'PRIVACY', 'FALSE_INFORMATION', 'OTHER')
    ),
    CONSTRAINT chk_review_report_status CHECK (
        status IN ('PENDING', 'ACCEPTED', 'REJECTED')
    ),
    CONSTRAINT chk_review_report_snapshot CHECK (
        snapshot_rating BETWEEN 1 AND 5
        AND snapshot_status IN ('PUBLISHED', 'HIDDEN')
    ),
    CONSTRAINT chk_review_report_decision CHECK (
        (status = 'PENDING'
            AND decision_note IS NULL
            AND decided_by_admin_id IS NULL
            AND decided_at IS NULL)
        OR
        (status IN ('ACCEPTED', 'REJECTED')
            AND decided_by_admin_id IS NOT NULL
            AND decided_at IS NOT NULL)
    ),
    CONSTRAINT fk_review_reports_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_reports_reporter
        FOREIGN KEY (reporter_user_id) REFERENCES users (id),
    CONSTRAINT fk_review_reports_admin
        FOREIGN KEY (decided_by_admin_id) REFERENCES admin_user (id),
    INDEX idx_review_reports_status_created (status, created_at, id),
    INDEX idx_review_reports_created (created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review_helpful_votes (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id   BIGINT      NOT NULL,
    user_id     BIGINT      NOT NULL,
    created_at  DATETIME(6) NOT NULL,

    CONSTRAINT uq_review_helpful_review_user UNIQUE (review_id, user_id),
    CONSTRAINT fk_review_helpful_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_helpful_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_review_helpful_user_review (user_id, review_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE review_images (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id   BIGINT        NOT NULL,
    image_url   VARCHAR(512)  NOT NULL,
    sort_order  INT           NOT NULL,
    created_at  DATETIME(6)   NOT NULL,

    CONSTRAINT uq_review_images_order UNIQUE (review_id, sort_order),
    CONSTRAINT uq_review_images_url UNIQUE (image_url),
    CONSTRAINT chk_review_images_order CHECK (sort_order BETWEEN 0 AND 4),
    CONSTRAINT fk_review_images_review
        FOREIGN KEY (review_id) REFERENCES reviews (id) ON DELETE CASCADE,
    INDEX idx_review_images_review (review_id, sort_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
