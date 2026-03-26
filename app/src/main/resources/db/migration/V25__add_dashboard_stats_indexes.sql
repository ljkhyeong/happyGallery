-- ==========================================================
-- V25: 대시보드 통계 집계 쿼리 최적화 인덱스
-- ==========================================================

-- 주문 매출 집계: paid_at 범위 스캔 (sargable WHERE paid_at >= ? AND paid_at < ?)
CREATE INDEX idx_orders_paid_at ON orders (paid_at);

-- 예약 입금 집계: deposit_paid_at 범위 스캔
CREATE INDEX idx_bookings_deposit_paid_at ON bookings (deposit_paid_at);

-- 이용권 매출 집계: purchased_at 범위 스캔
CREATE INDEX idx_pass_purchases_purchased_at ON pass_purchases (purchased_at);

-- 인기 상품 집계: order_id로 JOIN 후 product_id 집계 + unit_price·qty 커버링
CREATE INDEX idx_order_items_order_product ON order_items (order_id, product_id, unit_price, qty);
