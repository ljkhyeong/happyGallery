ALTER TABLE orders
    ADD COLUMN product_amount BIGINT NOT NULL DEFAULT 0 AFTER total_amount,
    ADD COLUMN coupon_discount_amount BIGINT NOT NULL DEFAULT 0 AFTER shipping_fee,
    ADD COLUMN reward_used_amount BIGINT NOT NULL DEFAULT 0 AFTER coupon_discount_amount,
    ADD COLUMN pg_paid_amount BIGINT NOT NULL DEFAULT 0 AFTER reward_used_amount,
    ADD COLUMN reward_earn_base BIGINT NOT NULL DEFAULT 0 AFTER pg_paid_amount,
    ADD COLUMN issued_coupon_id BIGINT NULL AFTER reward_earn_base;

UPDATE orders
SET product_amount = total_amount - shipping_fee,
    coupon_discount_amount = 0,
    reward_used_amount = 0,
    pg_paid_amount = total_amount,
    reward_earn_base = total_amount - shipping_fee;

ALTER TABLE orders
    ADD CONSTRAINT chk_orders_benefit_amounts CHECK (
        product_amount BETWEEN 1 AND 9007199254740991
        AND coupon_discount_amount BETWEEN 0 AND product_amount
        AND reward_used_amount BETWEEN 0 AND product_amount - coupon_discount_amount
        AND total_amount = product_amount + shipping_fee - coupon_discount_amount
        AND pg_paid_amount = total_amount - reward_used_amount
        AND reward_earn_base = product_amount - coupon_discount_amount - reward_used_amount
        AND ((issued_coupon_id IS NULL AND coupon_discount_amount = 0)
             OR (issued_coupon_id IS NOT NULL AND coupon_discount_amount > 0))
    );

ALTER TABLE order_items
    ADD COLUMN gross_amount BIGINT NOT NULL DEFAULT 0 AFTER unit_price,
    ADD COLUMN coupon_discount_amount BIGINT NOT NULL DEFAULT 0 AFTER gross_amount,
    ADD COLUMN reward_used_amount BIGINT NOT NULL DEFAULT 0 AFTER coupon_discount_amount,
    ADD COLUMN net_paid_amount BIGINT NOT NULL DEFAULT 0 AFTER reward_used_amount;

UPDATE order_items
SET gross_amount = qty * unit_price,
    coupon_discount_amount = 0,
    reward_used_amount = 0,
    net_paid_amount = qty * unit_price;

ALTER TABLE order_items
    ADD CONSTRAINT chk_order_items_benefit_amounts CHECK (
        gross_amount BETWEEN 1 AND 9007199254740991
        AND coupon_discount_amount BETWEEN 0 AND gross_amount
        AND reward_used_amount BETWEEN 0 AND gross_amount - coupon_discount_amount
        AND net_paid_amount = gross_amount - coupon_discount_amount - reward_used_amount
    );
