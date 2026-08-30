ALTER TABLE inventory
    ADD CONSTRAINT chk_inventory_quantity_nonnegative CHECK (quantity >= 0);

CREATE TABLE inventory_adjustments
(
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id           BIGINT       NOT NULL,
    type                 VARCHAR(10)  NOT NULL COMMENT 'INCREASE | DECREASE',
    quantity             INT          NOT NULL,
    quantity_before      INT          NOT NULL,
    quantity_after       INT          NOT NULL,
    reason               VARCHAR(500) NOT NULL,
    adjusted_by_admin_id BIGINT       NULL,
    adjusted_by          VARCHAR(100) NOT NULL,
    adjusted_at          DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inventory_adjustment_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT chk_inventory_adjustment_quantity CHECK (quantity > 0),
    CONSTRAINT chk_inventory_adjustment_quantity_before CHECK (quantity_before >= 0),
    CONSTRAINT chk_inventory_adjustment_quantity_after CHECK (quantity_after >= 0),
    CONSTRAINT chk_inventory_adjustment_reason CHECK (CHAR_LENGTH(TRIM(reason)) > 0),
    CONSTRAINT chk_inventory_adjustment_delta CHECK (
        (type = 'INCREASE' AND quantity_after = quantity_before + quantity)
        OR (type = 'DECREASE' AND quantity_after = quantity_before - quantity)
    ),
    INDEX idx_inventory_adjustment_product_time (product_id, adjusted_at, id)
);
