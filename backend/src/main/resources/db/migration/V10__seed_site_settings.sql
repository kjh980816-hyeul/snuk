-- 사이트 노출용 설정 시드 — 어드민 "설정" 탭에서 코드 수정 없이 변경 (ADR-003).
-- 값 '-' = 미설정(프론트에서 기본값 사용). setting_value 는 NOT NULL 이라 빈 문자열 대신 '-' 사용.
INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES
  ('LIVE_CHANNEL_ID', '-', '생방송 페이지 공식 치지직 채널 ID ("-"=미설정, 준비중 표시)', CURRENT_TIMESTAMP),
  ('HERO_IMAGE_URL', 'https://images.unsplash.com/photo-1542751371-adc38448a05e?w=1600&q=85', '홈 히어로 배경 이미지 URL', CURRENT_TIMESTAMP),
  ('BANNER_GOODS_URL', 'https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?w=1600&q=80', '굿즈샵 상단 배너 이미지 URL', CURRENT_TIMESTAMP),
  ('BANNER_PARTNERS_URL', 'https://images.unsplash.com/photo-1521737604893-d14cc237f11d?w=1600&q=80', '협력사 상단 배너 이미지 URL', CURRENT_TIMESTAMP);
