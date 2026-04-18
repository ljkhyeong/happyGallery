-- 문서와 HANDOFF 기준의 기본 관리자 계정(admin / admin1234)으로 정합화
UPDATE admin_user
SET password_hash = '$2a$10$XGsP.NqY9W2IXXvAi4QmKup/lGf6Muscc6iPMae1VOQ8NU3l4zLRK'
WHERE username = 'admin'
  AND password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';
