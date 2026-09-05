CREATE TABLE group_inquiries (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NULL,
    source VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    details_enc TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_group_inquiry_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_group_inquiry_status CHECK (status IN ('RECEIVED','CONSULTING','CONFIRMED','CLOSED')),
    CONSTRAINT chk_group_inquiry_source CHECK (source IN ('WEBSITE','EXTERNAL')),
    INDEX idx_group_inquiry_user (user_id, created_at, id),
    INDEX idx_group_inquiry_status (status, created_at, id),
    INDEX idx_group_inquiry_created (created_at, id)
);
CREATE TABLE group_inquiry_activities (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    inquiry_id BIGINT NOT NULL,
    admin_id BIGINT NULL,
    from_status VARCHAR(20) NULL,
    to_status VARCHAR(20) NOT NULL,
    note_enc TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_group_activity_inquiry FOREIGN KEY (inquiry_id) REFERENCES group_inquiries(id) ON DELETE CASCADE,
    INDEX idx_group_activity_inquiry (inquiry_id, id)
);
