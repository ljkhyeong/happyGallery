-- 비가역 호환 경계: HMAC 입력 형식이 바뀌므로 이 migration 뒤에는 V102 이전 binary를
-- 현재 DB에 연결하지 않는다. 적용 전 app 쓰기 중단과 복구 가능한 DB 백업이 필요하다.
ALTER TABLE phone_verifications
    ADD COLUMN purpose VARCHAR(40) NULL AFTER phone_hmac;

-- 기존 HMAC은 purpose를 포함하지 않아 새 계약으로 검증할 수 없으므로 명시적으로 폐기한다.
UPDATE phone_verifications
SET verified = TRUE
WHERE verified = FALSE;

UPDATE phone_verifications
SET purpose = 'GUEST_BOOKING'
WHERE purpose IS NULL;

ALTER TABLE phone_verifications
    MODIFY COLUMN purpose VARCHAR(40) NOT NULL;

CREATE INDEX idx_phone_verifications_phone_purpose_id
    ON phone_verifications (phone_hmac, purpose, id);
