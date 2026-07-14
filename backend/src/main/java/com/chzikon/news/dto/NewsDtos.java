package com.chzikon.news.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.review.domain.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class NewsDtos {

    private NewsDtos() {
    }

    public record NewsCreateRequest(
            @NotBlank @Size(max = 200) String title,
            String content,
            @Size(max = 512) String thumbnailUrl
    ) {
    }

    /** 공개 노출용 — 기자 닉네임/프사 포함. */
    public record NewsResponse(
            Long id,
            String title,
            String content,
            String thumbnailUrl,
            Long authorId,
            String authorName,
            String authorImageUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static NewsResponse of(Post p, Member author) {
            return new NewsResponse(p.getId(), p.getTitle(), p.getContent(), p.getThumbnailUrl(),
                    p.getMemberId(),
                    author != null ? author.getNickname() : "SNUK 기자",
                    author != null ? author.getProfileImageUrl() : null,
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
