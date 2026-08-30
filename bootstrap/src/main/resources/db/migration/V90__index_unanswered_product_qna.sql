CREATE INDEX idx_product_qna_unanswered_created
    ON product_qna (replied_at, created_at DESC, id DESC);
