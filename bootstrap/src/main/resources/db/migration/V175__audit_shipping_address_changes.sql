CREATE TABLE shipping_address_changes (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NULL,
    guest_id BIGINT NULL,
    before_address_enc VARCHAR(4096) NOT NULL,
    after_address_enc VARCHAR(4096) NOT NULL,
    changed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_address_change_order FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_address_change_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_address_change_guest FOREIGN KEY (guest_id) REFERENCES guests(id),
    CONSTRAINT ck_address_change_owner CHECK ((user_id IS NOT NULL) <> (guest_id IS NOT NULL)),
    INDEX idx_address_change_order (order_id, changed_at, id)
);
