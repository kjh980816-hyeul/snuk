-- 크루 정적 페이지 목록(어드민 "크루 페이지" 탭) — JSON [{name, path}]. 페이지 자체는 nginx /crew/ 정적 서빙.
INSERT INTO app_setting (setting_key, setting_value, description, updated_at)
SELECT 'CREW_PAGES', '[{"name":"뜨개동","path":"/crew/tteugae"}]', '크루 페이지 목록(JSON) — 어드민 크루 탭', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM app_setting WHERE setting_key = 'CREW_PAGES');
