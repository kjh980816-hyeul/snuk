-- 크루 페이지 자체 관리자 로그인(2026-08-24) — 크루 운영자는 스눅 ADMIN 계정 없이
-- 크루 전용 아이디/비밀번호로 페이지를 저장한다. 해시 = SHA-256(slug + ':' + 비밀번호) hex.
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
ALTER TABLE crew_page ADD COLUMN login_id VARCHAR(50) NULL;
ALTER TABLE crew_page ADD COLUMN login_pw_hash VARCHAR(100) NULL;

-- 뜨개동 계정 시드(admin / 요청된 비밀번호). 행이 없으면 빈 데이터로 먼저 생성(프론트는 {} 를 폴백 처리).
INSERT INTO crew_page (slug, data, updated_at)
SELECT 'tteugae', '{}', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM crew_page WHERE slug = 'tteugae');

UPDATE crew_page
SET login_id = 'admin',
    login_pw_hash = 'b314dde16de149669c23d7335dbaff39a7cd0c42532deef987b59efce9699a62'
WHERE slug = 'tteugae';
