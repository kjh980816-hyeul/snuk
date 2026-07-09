-- =====================================================================
-- V9 팔로우 + 스트리머 개인 게시판
-- member_follow : 회원 → 스트리머 팔로우 (1인 1팔로우 unique)
-- streamer_post : 스트리머별 개인 게시판 글. 작성=로그인 회원 누구나,
--                 삭제=작성자 본인 + 해당 스트리머 + ADMIN (서비스 강제)
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
-- =====================================================================

CREATE TABLE member_follow (
    id          BIGINT   AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT   NOT NULL,
    streamer_id BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL,
    CONSTRAINT uq_member_follow UNIQUE (follower_id, streamer_id),
    CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES member (id),
    CONSTRAINT fk_follow_streamer FOREIGN KEY (streamer_id) REFERENCES member (id)
);
CREATE INDEX idx_follow_streamer ON member_follow (streamer_id);

CREATE TABLE streamer_post (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    streamer_id BIGINT       NOT NULL,
    author_id   BIGINT       NOT NULL,
    title       VARCHAR(200) NOT NULL,
    content     TEXT,
    created_at  DATETIME     NOT NULL,
    CONSTRAINT fk_spost_streamer FOREIGN KEY (streamer_id) REFERENCES member (id),
    CONSTRAINT fk_spost_author   FOREIGN KEY (author_id)   REFERENCES member (id)
);
CREATE INDEX idx_spost_streamer ON streamer_post (streamer_id, created_at);
