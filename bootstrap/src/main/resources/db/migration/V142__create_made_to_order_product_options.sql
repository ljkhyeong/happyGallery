CREATE TABLE product_option_groups
(
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id             BIGINT       NOT NULL,
    option_key             VARCHAR(64)  NOT NULL,
    option_type            VARCHAR(10)  NOT NULL COMMENT 'SELECT | TEXT',
    name                   VARCHAR(25)  NOT NULL,
    required               BOOLEAN      NOT NULL,
    sort_order             INT          NOT NULL,
    input_placeholder      VARCHAR(100) NULL,
    input_max_length       INT          NULL,
    input_price_adjustment BIGINT       NULL,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at             DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_option_group_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_product_option_group_key UNIQUE (product_id, option_key),
    CONSTRAINT chk_product_option_group_sort CHECK (sort_order >= 0),
    CONSTRAINT chk_product_option_group_type_fields CHECK (
        (
            option_type = 'SELECT'
            AND input_placeholder IS NULL
            AND input_max_length IS NULL
            AND input_price_adjustment IS NULL
        )
        OR (
            option_type = 'TEXT'
            AND input_max_length BETWEEN 1 AND 200
            AND input_price_adjustment >= 0
        )
    )
);

CREATE INDEX idx_product_option_groups_product
    ON product_option_groups (product_id, active, sort_order, id);

CREATE TABLE product_option_values
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id    BIGINT      NOT NULL,
    option_key  VARCHAR(64) NOT NULL,
    name        VARCHAR(25) NOT NULL,
    sort_order  INT         NOT NULL,
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_option_value_group
        FOREIGN KEY (group_id) REFERENCES product_option_groups (id),
    CONSTRAINT uq_product_option_value_key UNIQUE (group_id, option_key),
    CONSTRAINT chk_product_option_value_sort CHECK (sort_order >= 0)
);

CREATE INDEX idx_product_option_values_group
    ON product_option_values (group_id, active, sort_order, id);

CREATE TABLE product_variants
(
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT       NOT NULL,
    combination_key   VARCHAR(512) NOT NULL,
    price_adjustment  BIGINT       NOT NULL DEFAULT 0,
    quantity          INT          NOT NULL DEFAULT 0,
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    version           BIGINT       NOT NULL DEFAULT 0,
    created_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_product_variant_product
        FOREIGN KEY (product_id) REFERENCES products (id),
    CONSTRAINT uq_product_variant_combination UNIQUE (product_id, combination_key),
    CONSTRAINT chk_product_variant_quantity CHECK (quantity >= 0)
);

CREATE INDEX idx_product_variants_product
    ON product_variants (product_id, active, id);

CREATE TABLE product_variant_selections
(
    variant_id       BIGINT NOT NULL,
    option_group_id  BIGINT NOT NULL,
    option_value_id  BIGINT NOT NULL,
    sort_order       INT    NOT NULL,
    PRIMARY KEY (variant_id, option_group_id),
    CONSTRAINT fk_product_variant_selection_variant
        FOREIGN KEY (variant_id) REFERENCES product_variants (id),
    CONSTRAINT fk_product_variant_selection_group
        FOREIGN KEY (option_group_id) REFERENCES product_option_groups (id),
    CONSTRAINT fk_product_variant_selection_value
        FOREIGN KEY (option_value_id) REFERENCES product_option_values (id),
    CONSTRAINT chk_product_variant_selection_sort CHECK (sort_order >= 0)
);

INSERT INTO product_variants (
    product_id,
    combination_key,
    price_adjustment,
    quantity,
    active,
    version
)
SELECT p.id,
       'DEFAULT',
       0,
       i.quantity,
       TRUE,
       0
FROM products p
JOIN inventory i ON i.product_id = p.id
WHERE p.type = 'MADE_TO_ORDER';

ALTER TABLE cart_items
    ADD COLUMN product_variant_id BIGINT NULL AFTER product_id,
    ADD COLUMN line_key VARCHAR(64) NULL AFTER product_variant_id;

UPDATE cart_items ci
JOIN products p ON p.id = ci.product_id
JOIN product_variants pv
  ON pv.product_id = ci.product_id
 AND pv.combination_key = 'DEFAULT'
SET ci.product_variant_id = pv.id
WHERE p.type = 'MADE_TO_ORDER';

UPDATE cart_items
SET line_key = SHA2(CONCAT(
        'product=', product_id,
        '|variant=', COALESCE(product_variant_id, 0),
        '|inputs='
    ), 256);

ALTER TABLE cart_items
    DROP INDEX uq_cart_user_product,
    MODIFY COLUMN line_key VARCHAR(64) NOT NULL,
    ADD CONSTRAINT fk_cart_item_product_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    ADD CONSTRAINT uq_cart_user_line UNIQUE (user_id, line_key);

CREATE INDEX idx_cart_items_product_variant
    ON cart_items (product_variant_id);

CREATE TABLE cart_item_text_inputs
(
    cart_item_id    BIGINT       NOT NULL,
    option_group_id BIGINT       NOT NULL,
    option_key      VARCHAR(64)  NOT NULL,
    value           VARCHAR(200) NOT NULL,
    sort_order      INT          NOT NULL,
    PRIMARY KEY (cart_item_id, option_group_id),
    CONSTRAINT fk_cart_item_text_input_cart
        FOREIGN KEY (cart_item_id) REFERENCES cart_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_item_text_input_group
        FOREIGN KEY (option_group_id) REFERENCES product_option_groups (id),
    CONSTRAINT chk_cart_item_text_input_sort CHECK (sort_order >= 0)
);

ALTER TABLE order_items
    ADD COLUMN product_variant_id BIGINT NULL AFTER product_id,
    ADD COLUMN base_price BIGINT NULL AFTER unit_price,
    ADD COLUMN variant_price_adjustment BIGINT NOT NULL DEFAULT 0 AFTER base_price,
    ADD COLUMN text_option_price_adjustment BIGINT NOT NULL DEFAULT 0 AFTER variant_price_adjustment;

UPDATE order_items oi
JOIN product_variants pv
  ON pv.product_id = oi.product_id
 AND pv.combination_key = 'DEFAULT'
SET oi.product_variant_id = pv.id
WHERE oi.product_type = 'MADE_TO_ORDER';

UPDATE order_items
SET base_price = unit_price;

ALTER TABLE order_items
    MODIFY COLUMN base_price BIGINT NOT NULL,
    ADD CONSTRAINT fk_order_item_product_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variants (id),
    ADD CONSTRAINT chk_order_item_option_price_parts CHECK (
        base_price >= 1
        AND text_option_price_adjustment >= 0
        AND unit_price = base_price + variant_price_adjustment + text_option_price_adjustment
    );

CREATE INDEX idx_order_items_product_variant
    ON order_items (product_variant_id);

CREATE TABLE order_item_option_snapshots
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_item_id    BIGINT       NOT NULL,
    option_type      VARCHAR(10)  NOT NULL COMMENT 'SELECT | TEXT',
    group_name       VARCHAR(25)  NOT NULL,
    value            VARCHAR(200) NOT NULL,
    price_adjustment BIGINT       NOT NULL DEFAULT 0,
    sort_order       INT          NOT NULL,
    CONSTRAINT fk_order_item_option_snapshot_item
        FOREIGN KEY (order_item_id) REFERENCES order_items (id) ON DELETE CASCADE,
    CONSTRAINT uq_order_item_option_snapshot_order UNIQUE (order_item_id, sort_order),
    CONSTRAINT chk_order_item_option_snapshot_sort CHECK (sort_order >= 0),
    CONSTRAINT chk_order_item_option_snapshot_price CHECK (price_adjustment >= 0)
);

ALTER TABLE inventory_adjustments
    ADD COLUMN product_variant_id BIGINT NULL AFTER product_id,
    ADD CONSTRAINT fk_inventory_adjustment_product_variant
        FOREIGN KEY (product_variant_id) REFERENCES product_variants (id);

CREATE INDEX idx_inventory_adjustments_product_variant
    ON inventory_adjustments (product_variant_id, adjusted_at DESC, id DESC);

UPDATE inventory i
JOIN products p ON p.id = i.product_id
SET i.quantity = 0
WHERE p.type = 'MADE_TO_ORDER';
