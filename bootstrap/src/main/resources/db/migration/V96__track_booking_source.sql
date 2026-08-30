ALTER TABLE bookings
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'WEB' AFTER status,
    ADD CONSTRAINT chk_bookings_source
        CHECK (source IN ('WEB', 'PHONE', 'NAVER_TALK', 'KAKAO', 'VISIT'));
