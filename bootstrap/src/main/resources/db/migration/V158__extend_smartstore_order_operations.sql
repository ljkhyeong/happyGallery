ALTER TABLE smartstore_product_orders
    ADD COLUMN delivery_info_enc TEXT NULL AFTER product_option,
    ADD COLUMN place_order_status VARCHAR(20) NULL AFTER product_order_status,
    ADD COLUMN shipping_due_date DATETIME(6) NULL AFTER place_order_status,
    ADD COLUMN expected_delivery_method VARCHAR(40) NULL AFTER shipping_due_date,
    ADD COLUMN delivery_company VARCHAR(40) NULL AFTER expected_delivery_method,
    ADD COLUMN tracking_number VARCHAR(100) NULL AFTER delivery_company,
    ADD COLUMN unit_price BIGINT NULL AFTER tracking_number,
    ADD COLUMN payment_amount BIGINT NULL AFTER unit_price,
    ADD COLUMN payment_commission BIGINT NULL AFTER payment_amount,
    ADD COLUMN sale_commission BIGINT NULL AFTER payment_commission,
    ADD COLUMN channel_commission BIGINT NULL AFTER sale_commission,
    ADD COLUMN expected_settlement_amount BIGINT NULL AFTER channel_commission;
