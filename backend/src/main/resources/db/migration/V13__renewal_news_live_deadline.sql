-- =====================================================================
-- V13 개편: 스눅 뉴스(REPORTER) + 스트리머 컨텐츠 업로드 + 후기 마감/연장/경고
--          + 스포트라이트 승인제 + 메인 라이브 배너 설정
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
-- =====================================================================

-- 뉴스 매거진 카드 썸네일 (post 단일 테이블 분기, category=NEWS)
ALTER TABLE post ADD COLUMN thumbnail_url VARCHAR(512);

-- 스트리머 업로드 컨텐츠: 소유자(NULL = 어드민 등록)
ALTER TABLE campaign ADD COLUMN owner_member_id BIGINT;

-- 게임체험단 후기 마감(키 배정 시 +30일) / 1회 7일 연장 / 미작성 경고
ALTER TABLE campaign_application ADD COLUMN review_deadline DATETIME;
ALTER TABLE campaign_application ADD COLUMN deadline_extended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE campaign_application ADD COLUMN warned_at DATETIME;

-- 스포트라이트 승인제: 승인 전 미노출, 승인 시각부터 2시간 노출
ALTER TABLE spotlight ADD COLUMN is_approved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE spotlight ADD COLUMN approved_at DATETIME;

-- 메인 라이브 배너(히어로 아래) — 어드민 on/off + 방송 주소 + 제목
INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES
  ('LIVE_BANNER_ENABLED', '0', '메인 라이브 배너 노출(1=켜짐/0=꺼짐)', CURRENT_TIMESTAMP),
  ('LIVE_BANNER_URL', '-', '메인 라이브 배너 방송 주소', CURRENT_TIMESTAMP),
  ('LIVE_BANNER_TITLE', '-', '메인 라이브 배너 제목', CURRENT_TIMESTAMP);
