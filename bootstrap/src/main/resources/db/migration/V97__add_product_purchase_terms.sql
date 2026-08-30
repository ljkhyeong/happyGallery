ALTER TABLE products
    ADD COLUMN specification VARCHAR(2000) NULL
        COMMENT '재료, 크기, 고정 사양' AFTER image_url,
    ADD COLUMN care_instructions VARCHAR(2000) NULL
        COMMENT '사용 및 관리 방법' AFTER specification,
    ADD COLUMN production_lead_days INT NULL
        COMMENT '주문제작 예상 제작 기간(일)' AFTER care_instructions;

UPDATE products
SET specification = COALESCE(
        NULLIF(TRIM(LEFT(description, 2000)), ''),
        '기존 상품은 고정 사양을 확인한 뒤 판매를 재개해 주세요.'),
    production_lead_days = 14,
    status = 'INACTIVE'
WHERE type = 'MADE_TO_ORDER';

ALTER TABLE products
    ADD CONSTRAINT chk_products_purchase_terms CHECK (
        (
            type = 'MADE_TO_ORDER'
            AND specification IS NOT NULL
            AND CHAR_LENGTH(TRIM(specification)) > 0
            AND production_lead_days IS NOT NULL
            AND production_lead_days BETWEEN 1 AND 180
        )
        OR (
            type = 'READY_STOCK'
            AND production_lead_days IS NULL
        )
    );

ALTER TABLE order_items
    ADD COLUMN product_type VARCHAR(20) NULL
        COMMENT '결제 준비 시점 상품 유형 스냅샷; NULL은 V97 이전 주문' AFTER product_name,
    ADD COLUMN specification VARCHAR(2000) NULL
        COMMENT '결제 준비 시점 상품 고정 사양 스냅샷' AFTER product_type,
    ADD COLUMN care_instructions VARCHAR(2000) NULL
        COMMENT '결제 준비 시점 관리 방법 스냅샷' AFTER specification,
    ADD COLUMN production_lead_days INT NULL
        COMMENT '결제 준비 시점 제작 기간(일) 스냅샷' AFTER care_instructions,
    ADD CONSTRAINT chk_order_items_purchase_terms CHECK (
        product_type IS NULL
        OR (
            product_type = 'MADE_TO_ORDER'
            AND specification IS NOT NULL
            AND CHAR_LENGTH(TRIM(specification)) > 0
            AND production_lead_days IS NOT NULL
            AND production_lead_days BETWEEN 1 AND 180
        )
        OR (
            product_type = 'READY_STOCK'
            AND production_lead_days IS NULL
        )
    );
