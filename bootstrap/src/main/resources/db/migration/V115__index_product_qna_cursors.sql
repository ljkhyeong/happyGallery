ALTER TABLE product_qna
    DROP INDEX idx_product_qna_product_created,
    ADD INDEX idx_product_qna_product_created_id
        (product_id, created_at DESC, id DESC),
    ADD INDEX idx_product_qna_product_user_created_id
        (product_id, user_id, created_at DESC, id DESC);
