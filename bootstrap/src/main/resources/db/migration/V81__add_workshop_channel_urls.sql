ALTER TABLE workshop_profiles
    ADD COLUMN naver_talk_url VARCHAR(500) NULL AFTER naver_talk_enabled,
    ADD COLUMN naver_blog_url VARCHAR(500) NULL AFTER naver_talk_url,
    ADD COLUMN instagram_url VARCHAR(500) NULL AFTER naver_blog_url,
    ADD COLUMN smart_store_url VARCHAR(500) NULL AFTER instagram_url;

UPDATE workshop_profiles
SET name = COALESCE(name, '해피갤러리'),
    phone = COALESCE(phone, '010-9635-5608'),
    address_line1 = COALESCE(address_line1, '충북 충주시 계명대로 161'),
    address_line2 = COALESCE(address_line2, '1층'),
    map_url = COALESCE(map_url, 'https://m.place.naver.com/place/21668321'),
    business_registration_number = COALESCE(business_registration_number, '303-11-87052'),
    representative_name = COALESCE(representative_name, '홍지현'),
    email = COALESCE(email, 'ssi1972@naver.com'),
    mail_order_registration_number = COALESCE(mail_order_registration_number, '2011-충북 충주-127'),
    introduction = COALESCE(introduction, '해피갤러리는 빈티지 가죽공예, 레진아트, 플루이드아트, 톨페인팅, 냅킨아트, 양말목공예, 하바리움, 위빙, POP 원데이클래스부터 자격증반, 창업반을 운영합니다.'),
    kakao_talk_id = COALESCE(kakao_talk_id, 'ssim1972'),
    naver_talk_url = CASE
        WHEN naver_talk_enabled THEN 'https://talk.naver.com/w4xufy'
        ELSE NULL
    END,
    naver_blog_url = 'https://blog.naver.com/ssim1972',
    instagram_url = 'https://www.instagram.com/happygallery_by/',
    smart_store_url = 'https://smartstore.naver.com/happygallery'
WHERE id = 1;

ALTER TABLE workshop_profiles
    DROP COLUMN naver_talk_enabled;
