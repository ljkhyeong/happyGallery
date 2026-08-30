ALTER TABLE bookings
    DROP INDEX uq_bookings_access_token,
    ADD INDEX idx_bookings_access_token_created
        (access_token, created_at DESC, id DESC);
