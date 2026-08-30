CREATE INDEX idx_payment_attempt_receipt
    ON payment_attempt (context, fulfilled_domain_id, status);
