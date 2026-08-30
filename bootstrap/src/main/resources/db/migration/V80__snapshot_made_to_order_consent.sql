ALTER TABLE orders
    ADD COLUMN made_to_order_consent_version VARCHAR(30) NULL AFTER approval_deadline_at,
    ADD COLUMN made_to_order_consent_disclosure VARCHAR(1000) NULL AFTER made_to_order_consent_version,
    ADD COLUMN made_to_order_consent_at DATETIME(6) NULL AFTER made_to_order_consent_disclosure;
