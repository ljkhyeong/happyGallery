ALTER TABLE bookings
    DROP INDEX idx_bookings_user,
    ADD INDEX idx_bookings_user_created_id
        (user_id, created_at DESC, id DESC);
