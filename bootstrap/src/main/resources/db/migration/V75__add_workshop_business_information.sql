ALTER TABLE workshop_profiles
    ADD COLUMN business_registration_number VARCHAR(20) NULL AFTER parking_info,
    ADD COLUMN representative_name VARCHAR(100) NULL AFTER business_registration_number,
    ADD COLUMN email VARCHAR(254) NULL AFTER representative_name,
    ADD COLUMN mail_order_registration_number VARCHAR(100) NULL AFTER email,
    ADD COLUMN introduction VARCHAR(2000) NULL AFTER mail_order_registration_number,
    ADD COLUMN kakao_talk_id VARCHAR(100) NULL AFTER introduction,
    ADD COLUMN naver_talk_enabled BOOLEAN NOT NULL DEFAULT FALSE AFTER kakao_talk_id;

UPDATE workshop_profiles
SET name = '해피갤러리',
    phone = '010-9635-5608',
    address_line1 = '충북 충주시 계명대로 161',
    address_line2 = '1층',
    business_registration_number = '303-11-87052',
    representative_name = NULL,
    email = NULL,
    mail_order_registration_number = NULL,
    introduction = '해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, 양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.',
    kakao_talk_id = 'ssim1972',
    naver_talk_enabled = TRUE
WHERE id = 1;
