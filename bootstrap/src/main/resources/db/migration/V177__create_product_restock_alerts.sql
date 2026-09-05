ALTER TABLE product_variants ADD UNIQUE KEY uq_variant_product_restock (id, product_id);

CREATE TABLE product_restock_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_variant_id BIGINT NULL,
    option_label VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    active_key VARCHAR(100) NULL,
    created_at DATETIME(6) NOT NULL,
    notified_at DATETIME(6) NULL,
    canceled_at DATETIME(6) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_restock_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_restock_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_restock_variant_product FOREIGN KEY (product_variant_id, product_id)
        REFERENCES product_variants(id, product_id) ON DELETE CASCADE,
    CONSTRAINT ck_restock_status CHECK (status IN ('WAITING', 'QUEUED', 'NOTIFIED', 'CANCELED')),
    UNIQUE KEY uq_restock_active (active_key),
    INDEX idx_restock_user (user_id, id),
    INDEX idx_restock_status (status, id)
);
