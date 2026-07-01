-- =====================================================================
-- V1 초기 스키마 — 치직온(CHZIKON)
-- 확정 도메인: AUTH · CMP · REV · COL · ADM
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용 (ENGINE/CHARSET 절 없음)
-- enum 은 VARCHAR + @Enumerated(STRING) 으로 매핑 (이식성)
-- 운영 DB(MySQL)는 DEFAULT CHARSET=utf8mb4 로 생성 전제
-- =====================================================================

-- ---------- member : 회원 ----------
CREATE TABLE member (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    chzzk_channel_id  VARCHAR(128) NOT NULL,
    nickname          VARCHAR(100) NOT NULL,
    profile_image_url VARCHAR(512),
    follower_count    INT,                                  -- 마지막 로그인 조회값 (nullable)
    role              VARCHAR(20)  NOT NULL DEFAULT 'VIEWER', -- GUEST/VIEWER/STREAMER/ADMIN
    role_overridden   BOOLEAN      NOT NULL DEFAULT FALSE,    -- true 면 자동 재산정 제외
    created_at        DATETIME     NOT NULL,
    updated_at        DATETIME     NOT NULL,
    CONSTRAINT uq_member_chzzk_channel_id UNIQUE (chzzk_channel_id)
);

-- ---------- app_setting : 어드민 설정값(key-value) ----------
CREATE TABLE app_setting (
    setting_key   VARCHAR(100) PRIMARY KEY,   -- 예: STREAMER_FOLLOWER_THRESHOLD
    setting_value VARCHAR(512) NOT NULL,
    description   VARCHAR(255),
    updated_by    BIGINT,
    updated_at    DATETIME     NOT NULL
);

-- ---------- campaign : 캠페인 ----------
CREATE TABLE campaign (
    id                BIGINT        AUTO_INCREMENT PRIMARY KEY,
    title             VARCHAR(200)  NOT NULL,
    description       TEXT,
    game_name         VARCHAR(200),                          -- nullable (순수 콘텐츠형)
    promo_image_url   VARCHAR(512),
    event_date        DATE,
    apply_start       DATETIME,
    apply_end         DATETIME,
    status            VARCHAR(20)   NOT NULL DEFAULT 'SCHEDULED', -- SCHEDULED/OPEN/CLOSED
    distribution_type VARCHAR(20)   NOT NULL DEFAULT 'FCFS',      -- FCFS/APPROVAL
    key_mode          VARCHAR(20)   NOT NULL DEFAULT 'QUANTITY',  -- QUANTITY/UNIQUE_KEY
    total_slots       INT           NOT NULL DEFAULT 0,
    filled_slots      INT           NOT NULL DEFAULT 0,           -- FCFS 원자 차감 대상
    is_featured       BOOLEAN       NOT NULL DEFAULT FALSE,       -- 홈 대표 노출
    sort_order        INT           NOT NULL DEFAULT 0,
    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL
);
CREATE INDEX idx_campaign_status ON campaign (status);
CREATE INDEX idx_campaign_featured ON campaign (is_featured, status);

-- ---------- game_key : 게임 키 (UNIQUE_KEY 모드 전용) ----------
CREATE TABLE game_key (
    id                 BIGINT       AUTO_INCREMENT PRIMARY KEY,
    campaign_id        BIGINT       NOT NULL,
    key_value_enc      VARCHAR(1024) NOT NULL,                -- AES-256 암호문(base64). 평문 금지
    key_fingerprint    VARCHAR(64)  NOT NULL,                 -- 중복감지용 SHA-256 해시(평문 노출X)
    status             VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE/ASSIGNED/REVOKED
    assigned_member_id BIGINT,
    assigned_at        DATETIME,
    created_at         DATETIME     NOT NULL,
    CONSTRAINT fk_game_key_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id)
);
CREATE INDEX idx_game_key_campaign_status ON game_key (campaign_id, status);
CREATE UNIQUE INDEX uq_game_key_campaign_fingerprint ON game_key (campaign_id, key_fingerprint);

-- ---------- campaign_application : 참가 신청 ----------
CREATE TABLE campaign_application (
    id                BIGINT      AUTO_INCREMENT PRIMARY KEY,
    campaign_id       BIGINT      NOT NULL,
    member_id         BIGINT      NOT NULL,
    status            VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    follower_snapshot INT         NOT NULL,                   -- 신청 시점 팔로워 수(CMP-07)
    assigned_key_id   BIGINT,                                 -- UNIQUE_KEY 모드 배정 키
    applied_at        DATETIME    NOT NULL,
    decided_at        DATETIME,
    CONSTRAINT fk_app_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id),
    CONSTRAINT fk_app_member   FOREIGN KEY (member_id)   REFERENCES member (id),
    CONSTRAINT uq_app_campaign_member UNIQUE (campaign_id, member_id)  -- 1인 1신청(CMP-06)
);
CREATE INDEX idx_app_member_status ON campaign_application (member_id, status);
CREATE INDEX idx_app_campaign_status ON campaign_application (campaign_id, status);

-- ---------- post : 게시글(후기 포함) ----------
CREATE TABLE post (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    category    VARCHAR(20)  NOT NULL DEFAULT 'REVIEW', -- REVIEW/...
    campaign_id BIGINT,                                 -- 후기면 연결
    member_id   BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    is_hidden   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL,
    updated_at  DATETIME     NOT NULL,
    CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_post_category_campaign ON post (category, campaign_id);

-- ---------- collab_game : 콜라보 게임 카드 ----------
CREATE TABLE collab_game (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    thumbnail_url  VARCHAR(512),
    game_link_url  VARCHAR(512),
    review_link_url VARCHAR(512),
    sort_order     INT          NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL
);

-- ---------- content_video : 콘텐츠 영상 ----------
CREATE TABLE content_video (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    video_url     VARCHAR(512) NOT NULL,
    thumbnail_url VARCHAR(512),
    is_featured   BOOLEAN      NOT NULL DEFAULT FALSE,  -- 히어로 대표 영상
    sort_order    INT          NOT NULL DEFAULT 0,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL
);

-- ---------- client_logo : 클라이언트 로고 ----------
CREATE TABLE client_logo (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(200),
    logo_url   VARCHAR(512) NOT NULL,
    link_url   VARCHAR(512),
    sort_order INT          NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL
);

-- ---------- admin_log : 감사 로그 ----------
CREATE TABLE admin_log (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    actor_member_id BIGINT,
    action          VARCHAR(60)  NOT NULL,   -- CAMPAIGN_UPDATE, KEY_REVOKE, ROLE_OVERRIDE ...
    target_type     VARCHAR(60),
    target_id       BIGINT,
    detail          TEXT,                    -- 전후값 요약
    created_at      DATETIME     NOT NULL
);
CREATE INDEX idx_admin_log_created ON admin_log (created_at);
