CREATE TABLE cart_items (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    product_id BIGINT      NOT NULL,
    qty        INT         NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_cart_item_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_cart_item_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_cart_user_product UNIQUE (user_id, product_id)
);

CREATE INDEX idx_cart_items_user ON cart_items (user_id);
