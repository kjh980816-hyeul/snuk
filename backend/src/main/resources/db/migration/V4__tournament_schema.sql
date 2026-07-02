-- =====================================================================
-- V4 대회 스키마 — SNUK TOUR (MVP)
-- 캠페인 승인제(APPROVAL) 패턴 재사용: 신청=PENDING 접수, 승인 시 정원 차감.
-- 실시간 대진/브래킷은 범위 밖(PRD TOUR-01 미확정) — 결과는 result_text 로 운영자 입력.
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용 (ENGINE/CHARSET 절 없음)
-- =====================================================================

-- ---------- tournament : 대회 ----------
CREATE TABLE tournament (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    game_name        VARCHAR(200),
    banner_image_url VARCHAR(512),
    event_date       DATE,                                     -- 대회 개최일
    apply_start      DATETIME,
    apply_end        DATETIME,
    capacity         INT          NOT NULL DEFAULT 0,          -- 참가 정원
    filled_slots     INT          NOT NULL DEFAULT 0,          -- 승인된 참가자 수
    status           VARCHAR(20)  NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED/OPEN/CLOSED/DONE
    result_text      TEXT,                                     -- 대회 결과(운영자 입력, DONE 시 노출)
    is_featured      BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order       INT          NOT NULL DEFAULT 0,
    created_at       DATETIME     NOT NULL,
    updated_at       DATETIME     NOT NULL
);
CREATE INDEX idx_tournament_status ON tournament (status, sort_order);

-- ---------- tournament_participant : 참가 신청 ----------
-- UNIQUE(tournament_id, member_id) = 1인 1신청. 승인/거절은 어드민.
CREATE TABLE tournament_participant (
    id                BIGINT      AUTO_INCREMENT PRIMARY KEY,
    tournament_id     BIGINT      NOT NULL,
    member_id         BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING/APPROVED/REJECTED
    follower_snapshot INT         NOT NULL DEFAULT 0,          -- 신청 시점 팔로워 수(심사 참고)
    applied_at        DATETIME    NOT NULL,
    decided_at        DATETIME,
    CONSTRAINT uq_tournament_participant UNIQUE (tournament_id, member_id),
    CONSTRAINT fk_tp_tournament FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_tp_member     FOREIGN KEY (member_id)     REFERENCES member (id)
);
CREATE INDEX idx_tp_tournament ON tournament_participant (tournament_id, applied_at);
CREATE INDEX idx_tp_member ON tournament_participant (member_id, applied_at);
