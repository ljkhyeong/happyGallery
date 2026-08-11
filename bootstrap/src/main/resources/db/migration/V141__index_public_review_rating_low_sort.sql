ALTER TABLE reviews
    ADD INDEX idx_reviews_product_rating_low_public
        (product_id, status, deleted_at, rating ASC, created_at DESC, id DESC),
    ADD INDEX idx_reviews_class_rating_low_public
        (booking_class_id, status, deleted_at, rating ASC, created_at DESC, id DESC);
