ALTER TABLE payment_attempt
    MODIFY COLUMN payload_enc MEDIUMTEXT NULL
        COMMENT 'prepare 시점 context별 payload 암호문; 만료된 PENDING은 개인정보 제거를 위해 NULL';
