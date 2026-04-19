-- Product Q&A
CREATE TABLE product_qna (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id    BIGINT       NOT NULL,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    secret        BOOLEAN      NOT NULL DEFAULT FALSE,
    password_hash VARCHAR(60)  NULL,
    reply_content TEXT         NULL,
    replied_at    DATETIME(6)  NULL,
    replied_by    BIGINT       NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_qna_product FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT fk_product_qna_user    FOREIGN KEY (user_id)    REFERENCES users (id)
);

CREATE INDEX idx_product_qna_product_created ON product_qna (product_id, created_at DESC);
CREATE INDEX idx_product_qna_user ON product_qna (user_id);

-- 1:1 Inquiry
CREATE TABLE inquiry (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT         NOT NULL,
    reply_content TEXT         NULL,
    replied_at    DATETIME(6)  NULL,
    replied_by    BIGINT       NULL,
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_inquiry_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_inquiry_user_created ON inquiry (user_id, created_at DESC);
