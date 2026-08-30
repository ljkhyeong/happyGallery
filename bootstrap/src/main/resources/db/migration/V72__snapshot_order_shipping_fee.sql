ALTER TABLE orders
    ADD COLUMN shipping_fee BIGINT NOT NULL DEFAULT 0 AFTER total_amount,
    ADD CONSTRAINT chk_orders_shipping_fee
        CHECK (shipping_fee >= 0 AND shipping_fee <= total_amount);
