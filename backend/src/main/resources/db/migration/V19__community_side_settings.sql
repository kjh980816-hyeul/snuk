-- 커뮤니티 좌측 메뉴 설정 — 어드민 "커뮤니티" 탭에서 코드 수정 없이 변경.
-- COMMUNITY_LINKS = 게시판 위에 붙는 바로가기 묶음(JSON 배열 [{"label":"...","url":"..."}], 빈 배열이면 묶음 자체를 숨김)
-- 값 '-' = 미설정(기본 문구 사용). setting_value NOT NULL 이라 '-' 사용.
INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES
  ('COMMUNITY_LINKS_TITLE', '스눅 소식', '커뮤니티 좌측 바로가기 묶음 제목', CURRENT_TIMESTAMP),
  ('COMMUNITY_LINKS', '[{"label":"스눅 뉴스","url":"/news"},{"label":"공지 · 이벤트","url":"/campaigns"}]',
   '커뮤니티 좌측 바로가기 목록(JSON: label/url)', CURRENT_TIMESTAMP),
  ('COMMUNITY_BANNER_TITLE', '스트리머와 시청자가 함께 쓰는 스눅 라운지', '커뮤니티 상단 배너 제목', CURRENT_TIMESTAMP),
  ('COMMUNITY_BANNER_SUB', 'SNUK COMMUNITY', '커뮤니티 상단 배너 윗줄 문구', CURRENT_TIMESTAMP),
  ('COMMUNITY_BANNER_URL', '-', '커뮤니티 상단 배너 클릭 시 이동할 주소(미설정이면 클릭 없음)', CURRENT_TIMESTAMP);
