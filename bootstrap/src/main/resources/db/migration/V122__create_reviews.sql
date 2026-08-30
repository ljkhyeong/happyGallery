CREATE UNIQUE INDEX uq_order_items_review_source
    ON order_items (id, product_id);

CREATE UNIQUE INDEX uq_bookings_review_source
    ON bookings (id, class_id);

CREATE TABLE reviews (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT       NOT NULL,
    order_item_id          BIGINT       NULL,
    product_id             BIGINT       NULL,
    booking_id             BIGINT       NULL,
    booking_class_id       BIGINT       NULL,
    rating                 INT          NOT NULL,
    content                TEXT         NOT NULL,
    status                 VARCHAR(10)  NOT NULL DEFAULT 'PUBLISHED',
    hidden_reason          VARCHAR(500) NULL,
    hidden_at              DATETIME(6)  NULL,
    hidden_by_admin_id     BIGINT       NULL,
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                          ON UPDATE CURRENT_TIMESTAMP(6),
    version                BIGINT       NOT NULL DEFAULT 0,

    CONSTRAINT uq_reviews_order_item UNIQUE (order_item_id),
    CONSTRAINT uq_reviews_booking UNIQUE (booking_id),

    CONSTRAINT chk_reviews_source_pair CHECK (
        (order_item_id IS NOT NULL AND product_id IS NOT NULL
            AND booking_id IS NULL AND booking_class_id IS NULL)
        OR
        (order_item_id IS NULL AND product_id IS NULL
            AND booking_id IS NOT NULL AND booking_class_id IS NOT NULL)
    ),
    CONSTRAINT chk_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT chk_reviews_status CHECK (status IN ('PUBLISHED', 'HIDDEN')),
    CONSTRAINT chk_reviews_moderation CHECK (
        (status = 'PUBLISHED'
            AND hidden_reason IS NULL
            AND hidden_at IS NULL
            AND hidden_by_admin_id IS NULL)
        OR
        (status = 'HIDDEN'
            AND hidden_reason IS NOT NULL
            AND hidden_at IS NOT NULL
            AND hidden_by_admin_id IS NOT NULL)
    ),

    CONSTRAINT fk_reviews_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_reviews_order_item_product
        FOREIGN KEY (order_item_id, product_id)
        REFERENCES order_items (id, product_id),
    CONSTRAINT fk_reviews_booking_class
        FOREIGN KEY (booking_id, booking_class_id)
        REFERENCES bookings (id, class_id),
    CONSTRAINT fk_reviews_hidden_admin
        FOREIGN KEY (hidden_by_admin_id) REFERENCES admin_user (id),

    INDEX idx_reviews_product_public (product_id, status, created_at, id),
    INDEX idx_reviews_class_public (booking_class_id, status, created_at, id),
    INDEX idx_reviews_user_created (user_id, created_at, id),
    INDEX idx_reviews_admin_status_created (status, created_at, id),
    INDEX idx_reviews_created (created_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
