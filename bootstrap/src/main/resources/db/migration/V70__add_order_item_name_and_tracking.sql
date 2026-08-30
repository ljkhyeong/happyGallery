ALTER TABLE order_items
    ADD COLUMN product_name VARCHAR(255) NULL
        COMMENT '결제 준비 시점 상품명 스냅샷' AFTER product_id;

UPDATE order_items oi
    JOIN products p ON p.id = oi.product_id
SET oi.product_name = p.name
WHERE oi.product_name IS NULL;

ALTER TABLE order_items
    MODIFY COLUMN product_name VARCHAR(255) NOT NULL
        COMMENT '결제 준비 시점 상품명 스냅샷';

ALTER TABLE fulfillments
    ADD COLUMN carrier VARCHAR(50) NULL
        COMMENT '택배사' AFTER shipping_address_enc,
    ADD COLUMN tracking_number VARCHAR(100) NULL
        COMMENT '운송장 번호' AFTER carrier,
    ADD CONSTRAINT chk_fulfillment_tracking_pair CHECK (
        (type = 'SHIPPING'
            AND ((carrier IS NULL AND tracking_number IS NULL)
                OR (carrier IS NOT NULL AND tracking_number IS NOT NULL)))
        OR (type = 'PICKUP' AND carrier IS NULL AND tracking_number IS NULL)
    );
