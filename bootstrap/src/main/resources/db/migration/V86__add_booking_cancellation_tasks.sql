CREATE TABLE booking_cancellation_tasks (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    booking_id            BIGINT       NOT NULL,
    task_type             VARCHAR(30)  NOT NULL,
    status                VARCHAR(15)  NOT NULL,
    reason                VARCHAR(500) NOT NULL,
    completed_by_admin_id BIGINT       NULL,
    created_at            DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at          DATETIME(6)  NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_booking_cancellation_task_booking_type
        UNIQUE (booking_id, task_type),
    CONSTRAINT fk_booking_cancellation_task_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

CREATE INDEX idx_booking_cancellation_task_pending
    ON booking_cancellation_tasks (status, created_at, id);
