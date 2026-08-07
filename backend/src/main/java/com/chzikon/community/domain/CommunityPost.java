package com.chzikon.community.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 커뮤니티 글.
 * 작성 = 로그인 회원 누구나 / 수정·삭제 = 작성자 본인 + ADMIN (서비스에서 강제).
 * 조회수·버프수·댓글수는 목록 성능을 위해 비정규화 보관.
 */
@Entity
@Table(name = "community_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "buff_count", nullable = false)
    private int buffCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CommunityPost(Long boardId, Long memberId, String title, String content) {
        this.boardId = boardId;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void edit(Long boardId, String title, String content) {
        if (boardId != null) this.boardId = boardId;
        if (title != null && !title.isBlank()) this.title = title.trim();
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        this.updatedAt = LocalDateTime.now();
    }

    public void addBuff(int delta) {
        this.buffCount = Math.max(0, this.buffCount + delta);
    }

    public void addComment(int delta) {
        this.commentCount = Math.max(0, this.commentCount + delta);
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
