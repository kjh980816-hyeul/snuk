-- =====================================================================
-- V8 공지사항 + 스포트라이트 — SNUK 시안 셸 연동
-- notice   : 운영자 공지(사이드바 노출). CRUD 어드민 전용, 조회 공개.
-- spotlight: 스트리머 방송 홍보. 등록 후 2시간 노출, 최대 2개 표시(조회측 LIMIT).
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용 (ENGINE/CHARSET 절 없음)
-- =====================================================================

-- ---------- notice : 공지사항 ----------
CREATE TABLE notice (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    title      VARCHAR(200) NOT NULL,
    content    TEXT,
    is_pinned  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_by BIGINT       NOT NULL,               -- 작성 어드민 id(감사용 — FK 없음: 로컬 시더 허용)
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL
);
CREATE INDEX idx_notice_list ON notice (is_pinned, created_at);

-- ---------- spotlight : 방송 홍보 ----------
-- 1인 1활성(조회 시 expires_at 검사). 삭제는 어드민.
CREATE TABLE spotlight (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    member_id  BIGINT       NOT NULL,
    title      VARCHAR(200) NOT NULL,
    platform   VARCHAR(20)  NOT NULL,               -- CHZZK/SOOP/YOUTUBE
    stream_url VARCHAR(512) NOT NULL,
    created_at DATETIME     NOT NULL,
    expires_at DATETIME     NOT NULL,               -- created_at + 2h
    CONSTRAINT fk_spotlight_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_spotlight_active ON spotlight (expires_at, created_at);
