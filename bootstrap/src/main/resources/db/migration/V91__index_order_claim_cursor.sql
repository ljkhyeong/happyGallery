CREATE INDEX idx_order_claims_requested
    ON order_claims (requested_at DESC, id DESC);
