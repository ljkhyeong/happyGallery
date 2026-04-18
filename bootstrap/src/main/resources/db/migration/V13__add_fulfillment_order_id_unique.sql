-- CR-P6: fulfillments.order_id unique 제약 추가 (주문당 fulfillment 1건 불변식)

-- 1. 혹시 중복 데이터가 있으면 최신 1건만 남기고 제거 (H2 + MySQL 호환)
DELETE FROM fulfillments
WHERE id NOT IN (
    SELECT max_id FROM (
        SELECT MAX(id) AS max_id FROM fulfillments GROUP BY order_id
    ) AS keep
);

-- 2. unique 제약 추가
ALTER TABLE fulfillments ADD CONSTRAINT uq_fulfillments_order_id UNIQUE (order_id);
