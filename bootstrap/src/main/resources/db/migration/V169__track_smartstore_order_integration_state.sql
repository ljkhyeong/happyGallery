ALTER TABLE smartstore_order_sync_state
    ADD COLUMN integration_enabled BOOLEAN NULL AFTER processing_started_at;
