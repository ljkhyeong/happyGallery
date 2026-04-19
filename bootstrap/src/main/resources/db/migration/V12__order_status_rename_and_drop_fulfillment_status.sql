-- CR-P5: OrderStatus 상태명에서 환불 완료 의미 제거 + Fulfillment.status 이중 관리 해소

-- 1. orders.status 이름 변경
UPDATE orders SET status = 'REJECTED'          WHERE status = 'REJECTED_REFUNDED';
UPDATE orders SET status = 'AUTO_REFUND_TIMEOUT' WHERE status = 'AUTO_REFUNDED_TIMEOUT';
UPDATE orders SET status = 'PICKUP_EXPIRED'    WHERE status = 'PICKUP_EXPIRED_REFUNDED';

-- 2. fulfillments.status 동일 변경 후 컬럼 제거 (Order.status가 단일 소스)
UPDATE fulfillments SET status = 'REJECTED'          WHERE status = 'REJECTED_REFUNDED';
UPDATE fulfillments SET status = 'AUTO_REFUND_TIMEOUT' WHERE status = 'AUTO_REFUNDED_TIMEOUT';
UPDATE fulfillments SET status = 'PICKUP_EXPIRED'    WHERE status = 'PICKUP_EXPIRED_REFUNDED';
ALTER TABLE fulfillments DROP COLUMN status;

-- 3. order_approvals.decision 폭 확장 (RESUME_PRODUCTION = 19자)
ALTER TABLE order_approvals
    MODIFY COLUMN decision VARCHAR(20) NOT NULL
    COMMENT 'APPROVE | REJECT | DELAY | AUTO_REFUND | PRODUCTION_COMPLETE | RESUME_PRODUCTION';
