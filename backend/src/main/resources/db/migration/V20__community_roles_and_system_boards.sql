-- =====================================================================
-- V20 커뮤니티 확장
--  1) 게시판별 글쓰기/열람 권한 (어드민에서 변경)
--     write_role : 이 게시판에 글을 쓸 수 있는 최소 등급 (VIEWER=로그인 회원 누구나)
--     read_role  : 이 게시판을 볼 수 있는 최소 등급 (GUEST=비로그인 포함 누구나)
--  2) 시스템 게시판(source) — 별도 테이블(공지/뉴스)을 커뮤니티에서 함께 보고 쓰기
--     NULL=일반 커뮤니티 글 / NOTICE=notice 테이블 / NEWS=post(category=NEWS)
--  3) 커뮤니티 배너 설정 키를 다른 페이지 배너와 같은 규칙(BANNER_*)으로 통일 → 어드민 "설정" 탭에서 관리
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
-- =====================================================================

ALTER TABLE community_board ADD COLUMN write_role VARCHAR(20) NOT NULL DEFAULT 'VIEWER';
ALTER TABLE community_board ADD COLUMN read_role  VARCHAR(20) NOT NULL DEFAULT 'GUEST';
ALTER TABLE community_board ADD COLUMN source     VARCHAR(20) NULL;

-- 스눅 소식 그룹 + 시스템 게시판 2종
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at, write_role, read_role, source)
VALUES (NULL, '스눅 소식', 0, TRUE, CURRENT_TIMESTAMP, 'ADMIN', 'GUEST', NULL);

INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at, write_role, read_role, source)
SELECT p.id, '공지사항', 1, TRUE, CURRENT_TIMESTAMP, 'ADMIN', 'GUEST', 'NOTICE'
FROM (SELECT id FROM community_board WHERE name = '스눅 소식' AND parent_id IS NULL) p;

INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at, write_role, read_role, source)
SELECT p.id, '스눅 뉴스', 2, TRUE, CURRENT_TIMESTAMP, 'REPORTER', 'GUEST', 'NEWS'
FROM (SELECT id FROM community_board WHERE name = '스눅 소식' AND parent_id IS NULL) p;

-- 배너 설정 키 통일 (COMMUNITY_BANNER_* → BANNER_COMMUNITY_*). 바로가기(COMMUNITY_LINKS*)는 그대로 커뮤니티 탭에서 관리.
INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES
  ('BANNER_COMMUNITY_URL', '-', '커뮤니티 페이지 배너 이미지 URL', CURRENT_TIMESTAMP),
  ('BANNER_COMMUNITY_TITLE', '스트리머와 시청자가 함께 쓰는 스눅 라운지', '커뮤니티 페이지 배너 제목', CURRENT_TIMESTAMP),
  ('BANNER_COMMUNITY_SUB', 'SNUK COMMUNITY', '커뮤니티 페이지 배너 문구', CURRENT_TIMESTAMP);

DELETE FROM app_setting WHERE setting_key IN
  ('COMMUNITY_BANNER_TITLE', 'COMMUNITY_BANNER_SUB', 'COMMUNITY_BANNER_URL');
