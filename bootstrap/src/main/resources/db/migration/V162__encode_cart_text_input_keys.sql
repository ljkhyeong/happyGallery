-- 구매 수량 차감에 사용하는 기존 항목 ID와 수량은 유지하고 식별 키만 전환한다.
SET @cart_previous_group_concat_max_len = @@SESSION.group_concat_max_len;
SET SESSION group_concat_max_len = 16384;

UPDATE cart_items ci
LEFT JOIN (
    SELECT cart_item_id,
           GROUP_CONCAT(
               CONCAT(option_key, '=', REPLACE(TO_BASE64(value), CHAR(10), ''), ';')
               ORDER BY sort_order, BINARY option_key SEPARATOR ''
           ) AS encoded_inputs
    FROM cart_item_text_inputs
    GROUP BY cart_item_id
) inputs ON inputs.cart_item_id = ci.id
SET ci.line_key = SHA2(CONCAT(
    'product=', ci.product_id,
    '|variant=', COALESCE(ci.product_variant_id, 0),
    '|inputs=', COALESCE(inputs.encoded_inputs, '')
), 256);

SET SESSION group_concat_max_len = @cart_previous_group_concat_max_len;
