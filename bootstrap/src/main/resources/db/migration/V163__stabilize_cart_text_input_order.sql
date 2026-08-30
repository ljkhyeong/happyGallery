-- 표시 순서와 무관하게 각인을 식별하되 결제 준비가 참조하는 항목 ID와 수량은 보존한다.
SET @cart_previous_group_concat_max_len = @@SESSION.group_concat_max_len;
SET SESSION group_concat_max_len = 16384;

CREATE TEMPORARY TABLE cart_stable_keys AS
SELECT id,
       CASE WHEN ROW_NUMBER() OVER (PARTITION BY user_id, stable_key ORDER BY id) = 1
            THEN stable_key
            ELSE CONCAT('legacy-cart-item:', id)
       END AS line_key
FROM (
    SELECT ci.id, ci.user_id,
           SHA2(CONCAT(
               'product=', ci.product_id,
               '|variant=', COALESCE(ci.product_variant_id, 0),
               '|inputs=', COALESCE(inputs.encoded_inputs, '')
           ), 256) AS stable_key
    FROM cart_items ci
    LEFT JOIN (
        SELECT cart_item_id,
               GROUP_CONCAT(
                   CONCAT(option_key, '=', REPLACE(TO_BASE64(value), CHAR(10), ''), ';')
                   ORDER BY BINARY option_key SEPARATOR ''
               ) AS encoded_inputs
        FROM cart_item_text_inputs
        GROUP BY cart_item_id
    ) inputs ON inputs.cart_item_id = ci.id
) canonical;

-- 기존 대표 키가 다른 행에 있어도 UNIQUE 충돌 없이 바꿀 수 있도록 임시 키를 사용한다.
UPDATE cart_items SET line_key = CONCAT('migration-v163:', id);
UPDATE cart_items ci
JOIN cart_stable_keys target ON target.id = ci.id
SET ci.line_key = target.line_key;

DROP TEMPORARY TABLE cart_stable_keys;
SET SESSION group_concat_max_len = @cart_previous_group_concat_max_len;
