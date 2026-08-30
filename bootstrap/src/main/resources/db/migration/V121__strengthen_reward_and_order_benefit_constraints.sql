-- 영구 DDL 전에 기존 적립금 예약 상태를 같은 CHECK 식으로 검증한다.
DROP TEMPORARY TABLE IF EXISTS reward_reservation_state_preflight;

CREATE TEMPORARY TABLE reward_reservation_state_preflight
(
    order_id        BIGINT      NULL,
    amount          BIGINT      NOT NULL,
    restored_amount BIGINT      NOT NULL,
    status           VARCHAR(20) NOT NULL,
    resolved_at      DATETIME(6) NULL,
    CONSTRAINT chk_v121_reward_reservation_state_preflight CHECK (
        (status = 'RESERVED'
            AND order_id IS NULL
            AND resolved_at IS NULL
            AND restored_amount = 0)
        OR
        (status = 'USED'
            AND order_id IS NOT NULL
            AND resolved_at IS NOT NULL
            AND restored_amount BETWEEN 0 AND amount)
        OR
        (status = 'RELEASED'
            AND order_id IS NULL
            AND resolved_at IS NOT NULL
            AND restored_amount = 0)
    )
);

INSERT INTO reward_reservation_state_preflight (
    order_id, amount, restored_amount, status, resolved_at
)
SELECT order_id, amount, restored_amount, status, resolved_at
FROM reward_reservations;

DROP TEMPORARY TABLE reward_reservation_state_preflight;

-- 주문 품목의 수량·단가·원금 산술도 영구 DDL 전에 검증한다.
DROP TEMPORARY TABLE IF EXISTS order_item_price_arithmetic_preflight;

CREATE TEMPORARY TABLE order_item_price_arithmetic_preflight
(
    qty          INT    NOT NULL,
    unit_price   BIGINT NOT NULL,
    gross_amount BIGINT NOT NULL,
    CONSTRAINT chk_v121_order_item_price_arithmetic_preflight CHECK (
        qty BETWEEN 1 AND 99
        AND unit_price BETWEEN 1 AND 9007199254740991
        AND gross_amount = qty * unit_price
    )
);

INSERT INTO order_item_price_arithmetic_preflight (qty, unit_price, gross_amount)
SELECT qty, unit_price, gross_amount
FROM order_items;

DROP TEMPORARY TABLE order_item_price_arithmetic_preflight;

-- 누락된 발급 쿠폰 참조는 LEFT JOIN 결과의 NULL을 NOT NULL 컬럼에 넣어 선검증한다.
DROP TEMPORARY TABLE IF EXISTS order_issued_coupon_fk_preflight;

CREATE TEMPORARY TABLE order_issued_coupon_fk_preflight
(
    issued_coupon_id BIGINT NOT NULL
);

INSERT INTO order_issued_coupon_fk_preflight (issued_coupon_id)
SELECT issued_coupon.id
FROM orders store_order
LEFT JOIN issued_coupons issued_coupon ON issued_coupon.id = store_order.issued_coupon_id
WHERE store_order.issued_coupon_id IS NOT NULL;

DROP TEMPORARY TABLE order_issued_coupon_fk_preflight;

ALTER TABLE reward_reservations
    ADD CONSTRAINT chk_reward_reservation_state CHECK (
        (status = 'RESERVED'
            AND order_id IS NULL
            AND resolved_at IS NULL
            AND restored_amount = 0)
        OR
        (status = 'USED'
            AND order_id IS NOT NULL
            AND resolved_at IS NOT NULL
            AND restored_amount BETWEEN 0 AND amount)
        OR
        (status = 'RELEASED'
            AND order_id IS NULL
            AND resolved_at IS NOT NULL
            AND restored_amount = 0)
    );

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_price_arithmetic CHECK (
        qty BETWEEN 1 AND 99
        AND unit_price BETWEEN 1 AND 9007199254740991
        AND gross_amount = qty * unit_price
    );

ALTER TABLE orders
    ADD CONSTRAINT fk_orders_issued_coupon
        FOREIGN KEY (issued_coupon_id) REFERENCES issued_coupons (id);
