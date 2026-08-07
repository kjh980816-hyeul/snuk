-- =====================================================================
-- V18 스눅 커뮤니티
--   community_board   : 게시판 트리(상위 그룹 + 하위 게시판). 어드민에서 CRUD
--   community_post    : 커뮤니티 글 (조회수/버프수/댓글수 비정규화)
--   community_comment : 댓글
--   community_buff    : 버프(추천) — UNIQUE(post,member) = 1인 1회 토글
--   community_report  : 신고 — UNIQUE(post,reporter) = 1인 1신고
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
-- =====================================================================

CREATE TABLE community_board (
    id         BIGINT      AUTO_INCREMENT PRIMARY KEY,
    parent_id  BIGINT      NULL,                       -- NULL = 상위 그룹
    name       VARCHAR(60) NOT NULL,
    sort_order INT         NOT NULL DEFAULT 0,
    is_visible BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at DATETIME    NOT NULL,
    CONSTRAINT fk_cboard_parent FOREIGN KEY (parent_id) REFERENCES community_board (id)
);
CREATE INDEX idx_cboard_sort ON community_board (parent_id, sort_order);

CREATE TABLE community_post (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    board_id      BIGINT       NOT NULL,
    member_id     BIGINT       NOT NULL,
    title         VARCHAR(200) NOT NULL,
    content       TEXT,
    view_count    INT          NOT NULL DEFAULT 0,
    buff_count    INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    is_hidden     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NOT NULL,
    CONSTRAINT fk_cpost_board  FOREIGN KEY (board_id)  REFERENCES community_board (id),
    CONSTRAINT fk_cpost_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_cpost_board   ON community_post (board_id, is_hidden, created_at);
CREATE INDEX idx_cpost_member  ON community_post (member_id, created_at);
CREATE INDEX idx_cpost_created ON community_post (is_hidden, created_at);

CREATE TABLE community_comment (
    id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT        NOT NULL,
    member_id  BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME      NOT NULL,
    CONSTRAINT fk_ccmt_post   FOREIGN KEY (post_id)   REFERENCES community_post (id),
    CONSTRAINT fk_ccmt_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_ccmt_post ON community_comment (post_id, created_at);

CREATE TABLE community_buff (
    id         BIGINT   AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT   NOT NULL,
    member_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uq_cbuff UNIQUE (post_id, member_id),
    CONSTRAINT fk_cbuff_post   FOREIGN KEY (post_id)   REFERENCES community_post (id),
    CONSTRAINT fk_cbuff_member FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE TABLE community_report (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    post_id     BIGINT       NOT NULL,
    reporter_id BIGINT       NOT NULL,
    reason      VARCHAR(500),
    created_at  DATETIME     NOT NULL,
    CONSTRAINT uq_creport UNIQUE (post_id, reporter_id),
    CONSTRAINT fk_creport_post FOREIGN KEY (post_id) REFERENCES community_post (id)
);
CREATE INDEX idx_creport_created ON community_report (created_at);

-- 기본 게시판 시드(운영자가 /admin 커뮤니티 탭에서 이름·순서·노출 변경 가능)
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at) VALUES
  (NULL, '자유',     1, TRUE, CURRENT_TIMESTAMP),
  (NULL, '정보공유', 2, TRUE, CURRENT_TIMESTAMP),
  (NULL, '플랫폼',   3, TRUE, CURRENT_TIMESTAMP);

-- 하위 게시판 — 명시 id 대신 부모 조회로 연결(H2/MySQL 자동증가 카운터 안전)
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '방송', 1, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '컴퓨터', 2, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '세팅', 3, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '조명', 4, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '카메라', 5, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '음향', 6, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '기타', 7, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '정보공유' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '치지직', 1, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '플랫폼' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '숲', 2, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '플랫폼' AND parent_id IS NULL) p;
INSERT INTO community_board (parent_id, name, sort_order, is_visible, created_at)
SELECT p.id, '씨미', 3, TRUE, CURRENT_TIMESTAMP
FROM (SELECT id FROM community_board WHERE name = '플랫폼' AND parent_id IS NULL) p;
