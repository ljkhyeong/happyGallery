ALTER TABLE orders
    DROP INDEX idx_orders_user_created,
    ADD INDEX idx_orders_user_created_id
        (user_id, created_at DESC, id DESC);
