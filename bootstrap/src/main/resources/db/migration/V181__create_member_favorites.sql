CREATE TABLE member_favorites (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NULL,
    class_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_favorite_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_favorite_class FOREIGN KEY (class_id) REFERENCES classes(id) ON DELETE CASCADE,
    CONSTRAINT chk_favorite_target CHECK ((product_id IS NOT NULL) + (class_id IS NOT NULL) = 1),
    UNIQUE KEY uk_favorite_product (user_id, product_id),
    UNIQUE KEY uk_favorite_class (user_id, class_id),
    INDEX idx_favorite_user_created (user_id, created_at, id)
);
