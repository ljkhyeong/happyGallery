ALTER TABLE reviews
    ADD INDEX idx_reviews_tombstone_retention (recreation_blocked, deleted_at, id);
