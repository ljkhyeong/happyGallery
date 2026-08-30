ALTER TABLE fulfillments
    ADD COLUMN carrier_code VARCHAR(30) NULL AFTER tracking_number,
    ADD COLUMN tracking_registration_status VARCHAR(20) NULL AFTER carrier_code,
    ADD COLUMN tracking_request_id VARCHAR(100) NULL AFTER tracking_registration_status,
    ADD COLUMN tracking_registration_attempts INT NOT NULL DEFAULT 0 AFTER tracking_request_id,
    ADD COLUMN tracking_next_attempt_at DATETIME(6) NULL AFTER tracking_registration_attempts,
    ADD COLUMN tracking_registration_started_at DATETIME(6) NULL AFTER tracking_next_attempt_at,
    ADD COLUMN tracking_last_error VARCHAR(500) NULL AFTER tracking_registration_started_at,
    ADD COLUMN tracking_status VARCHAR(30) NULL AFTER tracking_last_error,
    ADD COLUMN tracking_status_text VARCHAR(100) NULL AFTER tracking_status,
    ADD COLUMN tracking_updated_at DATETIME(6) NULL AFTER tracking_status_text,
    ADD INDEX idx_fulfillment_tracking_registration
        (tracking_registration_status, tracking_next_attempt_at, id);

CREATE TABLE shipment_tracking_events (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    occurred_at DATETIME(6) NOT NULL,
    status VARCHAR(30) NOT NULL,
    status_text VARCHAR(100) NOT NULL,
    location VARCHAR(200) NULL,
    description VARCHAR(500) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_shipment_tracking_event_order
        FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    INDEX idx_shipment_tracking_event_order_time (order_id, occurred_at, id)
);
