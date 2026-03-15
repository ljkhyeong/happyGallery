-- ==========================================================
-- V15: 고빈도 조회 쿼리용 복합 인덱스 추가
-- ==========================================================

-- 회원 주문 목록 (findByUserIdOrderByCreatedAtDesc)
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at DESC);

-- 게스트 주문 목록 (findByGuestIdOrderByCreatedAtDesc)
CREATE INDEX idx_orders_guest_created ON orders (guest_id, created_at DESC);

-- 상품 상태별 목록 (findByStatusOrderByCreatedAtDesc)
CREATE INDEX idx_products_status_created ON products (status, created_at DESC);

-- 회원 이용권 목록 (findByUserIdOrderByPurchasedAtDesc)
CREATE INDEX idx_pass_purchases_user_purchased ON pass_purchases (user_id, purchased_at DESC);
