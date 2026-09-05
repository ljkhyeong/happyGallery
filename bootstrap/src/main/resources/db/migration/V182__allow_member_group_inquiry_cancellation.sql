ALTER TABLE group_inquiries
    DROP CHECK chk_group_inquiry_status,
    ADD CONSTRAINT chk_group_inquiry_status CHECK (status IN ('RECEIVED','CONSULTING','CONFIRMED','CLOSED','CANCELED'));
