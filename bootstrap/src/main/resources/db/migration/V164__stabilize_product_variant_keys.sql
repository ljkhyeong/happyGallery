-- 표시 순서가 달랐던 동일 조합은 현재 판매 조합을 우선하며 모든 조합 번호와 재고를 보존한다.
CREATE TEMPORARY TABLE product_variant_stable_keys AS
SELECT id,
       CASE WHEN combination_rank = 1 THEN stable_key
            ELSE CONCAT('legacy-variant:', id)
       END AS combination_key,
       CASE WHEN combination_rank = 1 THEN active ELSE FALSE END AS active
FROM (
    SELECT canonical.*,
           ROW_NUMBER() OVER (
               PARTITION BY product_id, stable_key ORDER BY active DESC, id
           ) AS combination_rank
    FROM (
        SELECT variant.id, variant.product_id, variant.active,
               GROUP_CONCAT(parts.part ORDER BY BINARY SUBSTRING_INDEX(parts.part, '=', 1) SEPARATOR '|') AS stable_key
        FROM product_variants variant
        JOIN JSON_TABLE(
            CONCAT('["', REPLACE(variant.combination_key, '|', '","'), '"]'),
            '$[*]' COLUMNS (part VARCHAR(129) PATH '$')
        ) parts
        GROUP BY variant.id, variant.product_id, variant.active
    ) canonical
) ranked;

-- 기존 키와 새 키가 서로 바뀌어도 UNIQUE 충돌 없이 갱신한다.
UPDATE product_variants SET combination_key = CONCAT('migration-v164:', id);
UPDATE product_variants variant
JOIN product_variant_stable_keys target ON target.id = variant.id
SET variant.combination_key = target.combination_key,
    variant.active = target.active;

DROP TEMPORARY TABLE product_variant_stable_keys;
