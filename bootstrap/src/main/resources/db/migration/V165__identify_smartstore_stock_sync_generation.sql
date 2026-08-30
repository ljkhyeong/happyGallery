ALTER TABLE smartstore_stock_syncs
    ADD COLUMN generation VARCHAR(36) NOT NULL DEFAULT 'legacy';
