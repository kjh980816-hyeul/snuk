-- 공지사항·스눅 뉴스가 커뮤니티 "스눅 소식" 그룹의 실제 게시판(V20)이 되었으므로
-- 같은 자리에 있던 기본 바로가기 2개는 중복이라 비운다. (운영자가 /admin 커뮤니티 탭에서 다시 추가 가능)
UPDATE app_setting SET setting_value = '[]', updated_at = CURRENT_TIMESTAMP
WHERE setting_key = 'COMMUNITY_LINKS'
  AND setting_value = '[{"label":"스눅 뉴스","url":"/news"},{"label":"공지 · 이벤트","url":"/campaigns"}]';
