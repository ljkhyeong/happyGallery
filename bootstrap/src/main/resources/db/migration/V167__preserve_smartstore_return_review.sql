ALTER TABLE smartstore_product_orders
    ADD COLUMN return_reviewed_remain_quantity INT NULL
        COMMENT '마지막 반품 검수 당시 잔여 주문 수량';

UPDATE smartstore_product_orders
SET return_reviewed_remain_quantity = remain_quantity,
    updated_at = updated_at
WHERE product_order_status = 'RETURNED'
  AND attention_reason IS NULL;
