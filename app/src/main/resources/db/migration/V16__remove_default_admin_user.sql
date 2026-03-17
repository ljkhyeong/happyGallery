-- 기본 admin/admin1234 계정 삭제 — 프로덕션에서는 별도 bootstrap 필요
DELETE FROM admin_user WHERE username = 'admin';
