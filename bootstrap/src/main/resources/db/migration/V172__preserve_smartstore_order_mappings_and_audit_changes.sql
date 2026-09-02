CREATE TABLE smartstore_order_mapping_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    product_variant_id BIGINT NULL,
    origin_product_no BIGINT NOT NULL,
    option_id BIGINT NULL,
    enabled BOOLEAN NOT NULL,
    closed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_smartstore_order_mapping_history_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT fk_smartstore_order_mapping_history_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT ck_smartstore_order_mapping_history_target
        CHECK ((product_variant_id IS NULL AND option_id IS NULL)
            OR (product_variant_id IS NOT NULL AND option_id IS NOT NULL))
);

CREATE INDEX idx_smartstore_order_mapping_history_lookup
    ON smartstore_order_mapping_history (origin_product_no, option_id, closed_at);

CREATE INDEX idx_smartstore_order_mapping_history_product
    ON smartstore_order_mapping_history (product_id, origin_product_no, closed_at);

CREATE TABLE smartstore_inventory_mapping_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    action VARCHAR(20) NOT NULL,
    previous_origin_product_no BIGINT NULL,
    next_origin_product_no BIGINT NULL,
    previous_enabled BOOLEAN NULL,
    next_enabled BOOLEAN NULL,
    previous_option_mappings TEXT NULL,
    next_option_mappings TEXT NULL,
    previous_mapping_version BIGINT NULL,
    next_mapping_version BIGINT NULL,
    previous_origin_confirmed BOOLEAN NOT NULL,
    changed_by_admin_id BIGINT NULL,
    changed_by VARCHAR(100) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_smartstore_inventory_mapping_history_product
        FOREIGN KEY (product_id) REFERENCES products(id),
    CONSTRAINT ck_smartstore_inventory_mapping_history_action
        CHECK (action IN ('CREATED', 'UPDATED', 'ORIGIN_CHANGED', 'ENABLED', 'DISABLED', 'DELETED'))
);

CREATE INDEX idx_smartstore_inventory_mapping_history_product
    ON smartstore_inventory_mapping_history (product_id, changed_at DESC, id DESC);
