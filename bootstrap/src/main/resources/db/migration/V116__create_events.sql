CREATE TABLE events (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(200) NOT NULL,
    summary    VARCHAR(500) NOT NULL,
    content    TEXT         NOT NULL,
    image_url  VARCHAR(500) NULL,
    start_at   DATETIME     NOT NULL,
    end_at     DATETIME     NOT NULL,
    published  BOOLEAN      NOT NULL DEFAULT FALSE,
    featured   BOOLEAN      NOT NULL DEFAULT FALSE,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_events_period CHECK (start_at < end_at),
    INDEX idx_events_public (published, end_at, start_at),
    INDEX idx_events_featured (published, featured, end_at, start_at)
);

CREATE TABLE event_products (
    event_id   BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, product_id),
    INDEX idx_event_products_product (product_id),
    CONSTRAINT fk_event_products_event
        FOREIGN KEY (event_id) REFERENCES events (id) ON DELETE CASCADE,
    CONSTRAINT fk_event_products_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT
);
