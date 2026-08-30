CREATE TABLE smartstore_product_orders (
    product_order_id VARCHAR(30) NOT NULL,
    order_id VARCHAR(30) NOT NULL,
    origin_product_no BIGINT NOT NULL,
    item_no BIGINT NULL,
    product_id BIGINT NULL,
    product_variant_id BIGINT NULL,
    product_name VARCHAR(4000) NOT NULL,
    product_option VARCHAR(4000) NULL,
    product_order_status VARCHAR(40) NOT NULL,
    claim_type VARCHAR(40) NULL,
    claim_status VARCHAR(40) NULL,
    initial_quantity INT NOT NULL,
    remain_quantity INT NOT NULL,
    inventory_applied_quantity INT NOT NULL DEFAULT 0,
    attention_reason VARCHAR(30) NULL,
    last_changed_type VARCHAR(40) NOT NULL,
    payment_date DATETIME(6) NULL,
    last_changed_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (product_order_id),
    CONSTRAINT ck_smartstore_order_quantities
        CHECK (initial_quantity >= 0 AND remain_quantity >= 0 AND inventory_applied_quantity >= 0),
    CONSTRAINT ck_smartstore_order_attention
        CHECK (attention_reason IS NULL OR attention_reason IN (
            'MAPPING_REQUIRED', 'STOCK_SHORTAGE', 'RETURN_REVIEW', 'STATUS_REVIEW'
        ))
);

CREATE INDEX idx_smartstore_order_recent
    ON smartstore_product_orders (last_changed_at DESC, product_order_id);

CREATE INDEX idx_smartstore_order_attention
    ON smartstore_product_orders (attention_reason, last_changed_at DESC);

CREATE TABLE smartstore_order_sync_state (
    id BIGINT NOT NULL,
    last_changed_from DATETIME(6) NOT NULL,
    more_sequence VARCHAR(100) NULL,
    processing_started_at DATETIME(6) NULL,
    row_version BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT ck_smartstore_order_sync_singleton CHECK (id = 1)
);

INSERT INTO smartstore_order_sync_state (id, last_changed_from)
VALUES (1, DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 9 HOUR));
