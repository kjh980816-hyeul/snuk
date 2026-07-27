-- 파트너 스트리머 = 관리자 지정제. 공개 스트리머 섹션/라이브 목록은 이 플래그 기준으로 노출.
-- (등급 STREAMER 는 권한용 자동 산정 그대로 유지 — 노출과 분리)
ALTER TABLE member ADD COLUMN is_partner BOOLEAN NOT NULL DEFAULT FALSE;

-- 기존에 노출되던 STREAMER 등급 회원은 파트너로 백필(연속성)
UPDATE member SET is_partner = TRUE WHERE role = 'STREAMER';
