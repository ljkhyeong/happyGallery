CREATE INDEX idx_smartstore_stock_sync_reconciliation
    ON smartstore_stock_syncs (status, synced_at, product_id);
