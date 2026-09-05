ALTER TABLE group_inquiries ADD COLUMN next_contact_on DATE NULL;
CREATE INDEX idx_group_inquiries_next_contact ON group_inquiries(next_contact_on, id, status);
