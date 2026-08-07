package com.chzikon.community.dto;

import com.chzikon.community.domain.CommunityBoard;
import com.chzikon.community.domain.CommunityComment;
import com.chzikon.community.domain.CommunityPost;
import com.chzikon.member.domain.Member;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class CommunityDtos {

    private CommunityDtos() {
    }

    // ----- 게시판 -----

    public record BoardResponse(
            Long id,
            Long parentId,
            String name,
            int sortOrder,
            boolean visible
    ) {
        public static BoardResponse of(CommunityBoard b) {
            return new BoardResponse(b.getId(), b.getParentId(), b.getName(), b.getSortOrder(), b.isVisible());
        }
    }

    public record BoardRequest(
            Long parentId,
            @NotBlank @Size(max = 60) String name,
            Integer sortOrder,
            Boolean visible
    ) {
    }

    // ----- 글 -----

    public record PostRequest(
            @NotNull Long boardId,
            @NotBlank @Size(max = 200) String title,
            @Size(max = 20000) String content
    ) {
    }

    /** 목록 행. boardName = 하위 게시판이면 하위 이름, 아니면 게시판 이름. */
    public record PostSummary(
            Long id,
            Long boardId,
            String boardName,
            String groupName,
            String title,
            Long authorId,
            String authorName,
            String authorImageUrl,
            int viewCount,
            int buffCount,
            int commentCount,
            boolean hidden,
            LocalDateTime createdAt
    ) {
        public static PostSummary of(CommunityPost p, CommunityBoard board, CommunityBoard group, Member author) {
            return new PostSummary(p.getId(), p.getBoardId(),
                    board != null ? board.getName() : "삭제된 게시판",
                    group != null ? group.getName() : null,
                    p.getTitle(), p.getMemberId(),
                    author != null ? author.getNickname() : "탈퇴 회원",
                    author != null ? author.getProfileImageUrl() : null,
                    p.getViewCount(), p.getBuffCount(), p.getCommentCount(), p.isHidden(), p.getCreatedAt());
        }
    }

    public record PostPage(
            List<PostSummary> items,
            int page,
            int size,
            long total,
            int totalPages
    ) {
    }

    /** 상세. buffed/mine = 요청한 회원 기준(비로그인이면 false). */
    public record PostDetail(
            Long id,
            Long boardId,
            String boardName,
            String groupName,
            String title,
            String content,
            Long authorId,
            String authorName,
            String authorImageUrl,
            int viewCount,
            int buffCount,
            int commentCount,
            boolean buffed,
            boolean mine,
            boolean hidden,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static PostDetail of(CommunityPost p, CommunityBoard board, CommunityBoard group,
                                    Member author, boolean buffed, boolean mine) {
            return new PostDetail(p.getId(), p.getBoardId(),
                    board != null ? board.getName() : "삭제된 게시판",
                    group != null ? group.getName() : null,
                    p.getTitle(), p.getContent(), p.getMemberId(),
                    author != null ? author.getNickname() : "탈퇴 회원",
                    author != null ? author.getProfileImageUrl() : null,
                    p.getViewCount(), p.getBuffCount(), p.getCommentCount(),
                    buffed, mine, p.isHidden(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record BuffResponse(boolean buffed, int buffCount) {
    }

    // ----- 댓글 -----

    public record CommentRequest(
            @NotBlank @Size(max = 1000) String content
    ) {
    }

    public record CommentResponse(
            Long id,
            Long memberId,
            String nickname,
            String profileImageUrl,
            String content,
            LocalDateTime createdAt
    ) {
        public static CommentResponse of(CommunityComment c, Member author) {
            return new CommentResponse(c.getId(), c.getMemberId(),
                    author != null ? author.getNickname() : "탈퇴 회원",
                    author != null ? author.getProfileImageUrl() : null,
                    c.getContent(), c.getCreatedAt());
        }
    }

    // ----- 신고 -----

    public record ReportRequest(
            @Size(max = 500) String reason
    ) {
    }

    public record ReportView(
            Long id,
            Long postId,
            String postTitle,
            Long reporterId,
            String reporterName,
            String reason,
            boolean postHidden,
            LocalDateTime createdAt
    ) {
    }
}
