-- 기본 설정값 시드. 운영 중 어드민에서 변경 가능(코드 상수 금지, AUTH-04 / ADR-003).
INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES
  ('STREAMER_FOLLOWER_THRESHOLD', '50', 'STREAMER 권한 자동부여 팔로워 임계값', CURRENT_TIMESTAMP);
