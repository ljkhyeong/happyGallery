-- 상품 카테고리 컬럼 추가 (필터링용)
ALTER TABLE products ADD COLUMN category VARCHAR(50) NULL;

-- 상태 + 카테고리 복합 인덱스
CREATE INDEX idx_products_status_category ON products (status, category);
