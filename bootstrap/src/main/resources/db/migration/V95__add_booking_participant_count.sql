ALTER TABLE bookings
    ADD COLUMN participant_count INT NOT NULL DEFAULT 1 AFTER balance_amount,
    ADD CONSTRAINT chk_bookings_participant_count
        CHECK (participant_count >= 1 AND participant_count <= 8);
