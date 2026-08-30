CREATE INDEX idx_cart_merge_requests_created
    ON cart_merge_requests (created_at, user_id, idempotency_key);
