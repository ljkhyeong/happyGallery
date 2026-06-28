ALTER TABLE refunds
    ADD COLUMN pass_purchase_id BIGINT NULL COMMENT '8회권 환불' AFTER booking_id;

ALTER TABLE refunds
    ADD CONSTRAINT fk_refund_pass_purchase FOREIGN KEY (pass_purchase_id) REFERENCES pass_purchases (id);
