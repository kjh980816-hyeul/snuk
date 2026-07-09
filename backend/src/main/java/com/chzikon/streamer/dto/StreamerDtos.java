package com.chzikon.streamer.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.member.dto.StreamerPublicView;
import com.chzikon.streamer.domain.StreamerPost;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class StreamerDtos {

    private StreamerDtos() {
    }

    /** 스트리머 프로필 — 공개. following 은 비로그인 시 false. */
    public record StreamerProfileResponse(
            StreamerPublicView streamer,
            long followCount,
            boolean following
    ) {
    }

    public record StreamerPostRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 10_000) String content
    ) {
    }

    public record StreamerPostResponse(
            Long id,
            Long streamerId,
            Long authorId,
            String authorName,
            String authorImageUrl,
            String title,
            String content,
            LocalDateTime createdAt,
            boolean deletable
    ) {
        public static StreamerPostResponse of(StreamerPost p, Member author, boolean deletable) {
            return new StreamerPostResponse(p.getId(), p.getStreamerId(), p.getAuthorId(),
                    author != null ? author.getNickname() : "회원",
                    author != null ? author.getProfileImageUrl() : null,
                    p.getTitle(), p.getContent(), p.getCreatedAt(), deletable);
        }
    }
}
