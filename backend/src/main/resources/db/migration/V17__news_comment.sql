-- =====================================================================
-- V17 뉴스 댓글
-- news_comment : 뉴스 기사(post, category=NEWS) 댓글.
--                작성=로그인 회원 누구나, 삭제=작성자 본인 + ADMIN (서비스 강제)
-- portable DDL: H2(MODE=MySQL) / MySQL 8 공용
-- =====================================================================

CREATE TABLE news_comment (
    id         BIGINT        AUTO_INCREMENT PRIMARY KEY,
    post_id    BIGINT        NOT NULL,
    member_id  BIGINT        NOT NULL,
    content    VARCHAR(1000) NOT NULL,
    created_at DATETIME      NOT NULL,
    CONSTRAINT fk_newscmt_post   FOREIGN KEY (post_id)   REFERENCES post (id),
    CONSTRAINT fk_newscmt_member FOREIGN KEY (member_id) REFERENCES member (id)
);
CREATE INDEX idx_newscmt_post ON news_comment (post_id, created_at);
