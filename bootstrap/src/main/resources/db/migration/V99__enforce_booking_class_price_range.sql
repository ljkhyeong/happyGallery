-- 기존 데이터가 애플리케이션 결제 금액 계약을 만족하는지 영구 DDL 전에 검증한다.
DROP TEMPORARY TABLE IF EXISTS booking_class_price_preflight;

CREATE TEMPORARY TABLE booking_class_price_preflight (
    price BIGINT NOT NULL,
    CHECK (price BETWEEN 10 AND 9007199254740991)
);

INSERT INTO booking_class_price_preflight (price)
SELECT price
FROM classes;

DROP TEMPORARY TABLE booking_class_price_preflight;

ALTER TABLE classes
    ADD CONSTRAINT chk_classes_price_range
        CHECK (price BETWEEN 10 AND 9007199254740991);
