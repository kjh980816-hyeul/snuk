-- V22: 컨텐츠(캠페인/체험단) 신청 질문·답변 — 대회(V15) 패턴 미러
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용

ALTER TABLE campaign ADD COLUMN apply_questions TEXT NULL;
ALTER TABLE campaign_application ADD COLUMN answers TEXT NULL;
