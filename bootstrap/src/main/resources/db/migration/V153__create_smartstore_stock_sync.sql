CREATE TABLE smartstore_stock_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    product_variant_id BIGINT NULL,
    origin_product_no BIGINT NOT NULL,
    option_id BIGINT NULL,
    internal_target_key BIGINT AS (COALESCE(product_variant_id, 0)) STORED,
    external_target_key BIGINT AS (COALESCE(option_id, 0)) STORED,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_smartstore_stock_mapping_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_smartstore_stock_mapping_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT uk_smartstore_stock_mapping_product_variant
        UNIQUE (product_id, internal_target_key),
    CONSTRAINT uk_smartstore_stock_mapping_option
        UNIQUE (origin_product_no, external_target_key),
    CONSTRAINT ck_smartstore_stock_mapping_target
        CHECK ((product_variant_id IS NULL AND option_id IS NULL)
            OR (product_variant_id IS NOT NULL AND option_id IS NOT NULL))
);

CREATE INDEX idx_smartstore_stock_mapping_product
    ON smartstore_stock_mappings (product_id, enabled);

CREATE TABLE smartstore_stock_syncs (
    product_id BIGINT NOT NULL,
    request_version BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at DATETIME(6) NOT NULL,
    processing_started_at DATETIME(6) NULL,
    last_error VARCHAR(500) NULL,
    synced_at DATETIME(6) NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_id),
    CONSTRAINT fk_smartstore_stock_sync_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_smartstore_stock_sync_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'SYNCED', 'FAILED'))
);

CREATE INDEX idx_smartstore_stock_sync_due
    ON smartstore_stock_syncs (status, next_attempt_at, product_id);
