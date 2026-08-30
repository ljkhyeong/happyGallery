CREATE INDEX idx_notification_outbox_event_aggregate
    ON notification_outbox (event_type, aggregate_type, aggregate_id);
