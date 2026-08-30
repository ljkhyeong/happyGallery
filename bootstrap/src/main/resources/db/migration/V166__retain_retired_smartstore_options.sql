ALTER TABLE smartstore_stock_mappings
    ADD COLUMN retired BOOLEAN NOT NULL DEFAULT FALSE,
    DROP INDEX uk_smartstore_stock_mapping_product_variant,
    DROP COLUMN internal_target_key,
    ADD COLUMN internal_target_key BIGINT AS (
        CASE WHEN retired THEN NULL ELSE COALESCE(product_variant_id, 0) END
    ) STORED,
    ADD CONSTRAINT uk_smartstore_stock_mapping_product_variant
        UNIQUE (product_id, internal_target_key);
