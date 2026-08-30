ALTER TABLE events
    ADD COLUMN coupon_definition_id BIGINT NULL AFTER featured,
    ADD INDEX idx_events_coupon_definition (coupon_definition_id),
    ADD CONSTRAINT fk_events_coupon_definition
        FOREIGN KEY (coupon_definition_id) REFERENCES coupon_definitions (id);
