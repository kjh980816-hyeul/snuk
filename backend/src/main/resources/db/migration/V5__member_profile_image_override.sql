-- 프사 수동 변경 가드: TRUE 면 로그인 시 치지직 프사 동기화가 덮어쓰지 않음
ALTER TABLE member ADD COLUMN profile_image_overridden BOOLEAN NOT NULL DEFAULT FALSE;
