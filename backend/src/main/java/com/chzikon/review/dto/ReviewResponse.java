package com.chzikon.review.dto;

import com.chzikon.review.domain.Post;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long campaignId,
        Long memberId,
        String title,
        String content,
        LocalDateTime createdAt
) {
    public static ReviewResponse from(Post p) {
        return new ReviewResponse(p.getId(), p.getCampaignId(), p.getMemberId(),
                p.getTitle(), p.getContent(), p.getCreatedAt());
    }
}
