ALTER TABLE classes
    ADD COLUMN capacity INT NOT NULL DEFAULT 8 AFTER buffer_min,
    ADD CONSTRAINT chk_classes_capacity CHECK (capacity >= 1);

ALTER TABLE bookings
    DROP CHECK chk_bookings_participant_count,
    ADD CONSTRAINT chk_bookings_participant_count CHECK (participant_count >= 1);
