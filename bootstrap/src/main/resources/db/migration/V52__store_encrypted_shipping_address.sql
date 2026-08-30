ALTER TABLE fulfillments
    ADD COLUMN shipping_address_enc VARCHAR(4096) NULL
        COMMENT '주문 시점 배송지 JSON AES-GCM 암호문' AFTER pickup_deadline_at;
