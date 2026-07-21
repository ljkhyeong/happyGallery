CREATE TABLE workshop_profiles (
    id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30) NULL,
    postal_code VARCHAR(20) NULL,
    address_line1 VARCHAR(200) NULL,
    address_line2 VARCHAR(200) NULL,
    business_hours VARCHAR(1000) NULL,
    map_url VARCHAR(500) NULL,
    parking_info VARCHAR(1000) NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_workshop_profiles_singleton CHECK (id = 1)
);

INSERT INTO workshop_profiles (id, name) VALUES (1, '해피갤러리');
