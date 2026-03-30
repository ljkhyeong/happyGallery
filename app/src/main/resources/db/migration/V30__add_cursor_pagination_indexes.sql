-- 커서 기반 페이지네이션용 복합 인덱스: tuple comparison (created_at, id) < (?, ?) 최적화
CREATE INDEX idx_orders_created_id ON orders (created_at DESC, id DESC);
CREATE INDEX idx_orders_status_created_id ON orders (status, created_at DESC, id DESC);
