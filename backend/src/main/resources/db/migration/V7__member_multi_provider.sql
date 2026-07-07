-- =====================================================================
-- V7 멀티 플랫폼 로그인 — 치지직(CHZZK) + 씨미(CIME) + 숲(SOOP)
-- member 식별을 chzzk_channel_id 단일 → (provider, channel_id) 복합으로 확장
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용 (DROP CONSTRAINT 는 MySQL 8.0.19+)
-- =====================================================================

ALTER TABLE member DROP CONSTRAINT uq_member_chzzk_channel_id;
ALTER TABLE member RENAME COLUMN chzzk_channel_id TO channel_id;
ALTER TABLE member ADD COLUMN provider VARCHAR(20) NOT NULL DEFAULT 'CHZZK';
ALTER TABLE member ADD CONSTRAINT uq_member_provider_channel UNIQUE (provider, channel_id);
