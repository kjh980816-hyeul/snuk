-- V23: 권한 신청 + 알림 + 스트리머 페이지 심화(공지/일정/위키) + 방송도우미 도구(명령어/룰렛/노래신청)
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용

-- 1) 권한 신청 (VIEWER -> STREAMER, 어드민 승인제)
CREATE TABLE role_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    message VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    decided_by BIGINT,
    created_at DATETIME NOT NULL,
    decided_at DATETIME
);
CREATE INDEX idx_role_request_member ON role_request (member_id, created_at);
CREATE INDEX idx_role_request_status ON role_request (status);

-- 2) 알림함
CREATE TABLE notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    body VARCHAR(500),
    link_path VARCHAR(200),
    read_at DATETIME,
    created_at DATETIME NOT NULL
);
CREATE INDEX idx_notification_member ON notification (member_id, created_at);

-- 3) 스트리머 페이지: 공지
CREATE TABLE streamer_notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT,
    important BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL
);
CREATE INDEX idx_streamer_notice ON streamer_notice (streamer_id, created_at);

-- 4) 스트리머 페이지: 방송 일정
CREATE TABLE streamer_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT NOT NULL,
    start_at DATETIME NOT NULL,
    title VARCHAR(200) NOT NULL,
    game VARCHAR(100),
    mates VARCHAR(300),
    created_at DATETIME NOT NULL
);
CREATE INDEX idx_streamer_schedule ON streamer_schedule (streamer_id, start_at);

-- 5) 스트리머 페이지: 위키 문서 (섹션 JSON [{"t":"제목","b":"본문"}])
CREATE TABLE streamer_wiki (
    streamer_id BIGINT PRIMARY KEY,
    sections TEXT,
    updated_at DATETIME NOT NULL
);

-- 6) 방송도우미: 채팅 명령어
CREATE TABLE streamer_command (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    response VARCHAR(500) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_streamer_command UNIQUE (streamer_id, name)
);

-- 7) 방송도우미: 후원 룰렛 항목
CREATE TABLE streamer_roulette_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT NOT NULL,
    label VARCHAR(100) NOT NULL,
    weight INT NOT NULL DEFAULT 1,
    sort_order INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_roulette ON streamer_roulette_item (streamer_id);

-- 8) 방송도우미: 노래 신청 대기열
CREATE TABLE streamer_song_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    decided_at DATETIME
);
CREATE INDEX idx_song_req ON streamer_song_request (streamer_id, status, created_at);
