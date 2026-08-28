CREATE TABLE booking_vacancy_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_id BIGINT NOT NULL,
    guest_id BIGINT NULL,
    user_id BIGINT NULL,
    access_token_hash VARCHAR(64) NULL,
    active_key VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL,
    notified_at DATETIME(6) NULL,
    canceled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_booking_vacancy_alerts_slot
        FOREIGN KEY (slot_id) REFERENCES slots (id),
    CONSTRAINT fk_booking_vacancy_alerts_guest
        FOREIGN KEY (guest_id) REFERENCES guests (id),
    CONSTRAINT fk_booking_vacancy_alerts_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT chk_booking_vacancy_alert_owner CHECK (
        (guest_id IS NOT NULL AND user_id IS NULL AND access_token_hash IS NOT NULL)
        OR (guest_id IS NULL AND user_id IS NOT NULL AND access_token_hash IS NULL)
    ),
    CONSTRAINT uq_booking_vacancy_alert_active UNIQUE (active_key),
    INDEX idx_booking_vacancy_alert_slot_status (slot_id, status, id)
);
