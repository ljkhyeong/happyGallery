ALTER TABLE orders
    DROP INDEX uq_orders_access_token,
    ADD INDEX idx_orders_access_token_created
        (access_token, created_at DESC, id DESC);
