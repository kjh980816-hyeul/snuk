-- V15: 포인트 / 스포트라이트 예정일시 / 대회 참가 질문·답변 / 게시글 신고 / 무료소스
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용

-- 1) 스포트라이트 방송 예정 일시(선택 입력)
ALTER TABLE spotlight ADD COLUMN scheduled_at DATETIME NULL;

-- 2) 포인트 — 하루 첫 로그인 적립, 스포트라이트 등록 시 차감
ALTER TABLE member ADD COLUMN points INT NOT NULL DEFAULT 0;
ALTER TABLE member ADD COLUMN last_daily_point_at DATE NULL;

-- 3) 대회·컨텐츠 참가 신청 커스텀 질문(주최자 작성, JSON 배열)/답변(신청자 작성, JSON 배열)
ALTER TABLE tournament ADD COLUMN apply_questions TEXT NULL;
ALTER TABLE tournament_participant ADD COLUMN answers TEXT NULL;

-- 4) 스트리머 게시판 글 신고
CREATE TABLE streamer_post_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500),
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_post_report UNIQUE (post_id, reporter_id)
);
CREATE INDEX idx_post_report_created ON streamer_post_report (created_at);

-- 5) 무료소스 자료실
CREATE TABLE free_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    file_url VARCHAR(512),
    image_url VARCHAR(512),
    uploader_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL
);
CREATE INDEX idx_free_resource_created ON free_resource (created_at);

-- 6) 포인트 설정 시드
INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES
  ('POINT_DAILY_AMOUNT', '10', '하루 첫 로그인 적립 포인트', CURRENT_TIMESTAMP),
  ('SPOTLIGHT_POINT_COST', '50', '스포트라이트 등록 차감 포인트(0=무료)', CURRENT_TIMESTAMP);
