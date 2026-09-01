ALTER TABLE smartstore_order_sync_state
    ADD COLUMN pending_activation_from DATETIME(6) NULL AFTER integration_enabled;
