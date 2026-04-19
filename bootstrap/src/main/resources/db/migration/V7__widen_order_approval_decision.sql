-- AUTO_REFUND(11자) 수용을 위해 decision 컬럼 길이 확장
ALTER TABLE order_approvals
    MODIFY COLUMN decision VARCHAR(20) NOT NULL COMMENT 'APPROVE | REJECT | DELAY | AUTO_REFUND';
