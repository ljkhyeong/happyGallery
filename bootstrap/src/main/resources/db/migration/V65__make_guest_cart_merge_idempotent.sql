CREATE TABLE cart_merge_requests (
    user_id BIGINT NOT NULL,
    idempotency_key CHAR(36) NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (user_id, idempotency_key),
    CONSTRAINT fk_cart_merge_requests_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
