CREATE INDEX idx_refunds_booking_created
    ON refunds (booking_id, created_at DESC, id DESC);

DROP INDEX uq_refunds_booking ON refunds;

ALTER TABLE booking_history
    MODIFY COLUMN action VARCHAR(30) NOT NULL;
