package com.chzikon.collab.dto;

import com.chzikon.collab.domain.ClientLogo;
import com.chzikon.collab.domain.CollabGame;
import com.chzikon.collab.domain.ContentVideo;
import jakarta.validation.constraints.NotBlank;

/** 콜라보/노출 도메인 요청·응답 DTO 모음. */
public final class CollabDtos {

    private CollabDtos() {
    }

    // ----- CollabGame -----
    public record CollabGameRequest(
            @NotBlank String name,
            String description,
            String thumbnailUrl,
            String gameLinkUrl,
            String reviewLinkUrl,
            int sortOrder
    ) {
    }

    public record CollabGameResponse(
            Long id, String name, String description, String thumbnailUrl,
            String gameLinkUrl, String reviewLinkUrl, int sortOrder
    ) {
        public static CollabGameResponse from(CollabGame g) {
            return new CollabGameResponse(g.getId(), g.getName(), g.getDescription(),
                    g.getThumbnailUrl(), g.getGameLinkUrl(), g.getReviewLinkUrl(), g.getSortOrder());
        }
    }

    // ----- ContentVideo -----
    public record ContentVideoRequest(
            @NotBlank String title,
            @NotBlank String videoUrl,
            String thumbnailUrl,
            boolean featured,
            int sortOrder
    ) {
    }

    public record ContentVideoResponse(
            Long id, String title, String videoUrl, String thumbnailUrl,
            boolean featured, int sortOrder
    ) {
        public static ContentVideoResponse from(ContentVideo v) {
            return new ContentVideoResponse(v.getId(), v.getTitle(), v.getVideoUrl(),
                    v.getThumbnailUrl(), v.isFeatured(), v.getSortOrder());
        }
    }

    // ----- ClientLogo -----
    public record ClientLogoRequest(
            String name,
            @NotBlank String logoUrl,
            String linkUrl,
            int sortOrder
    ) {
    }

    public record ClientLogoResponse(
            Long id, String name, String logoUrl, String linkUrl, int sortOrder
    ) {
        public static ClientLogoResponse from(ClientLogo c) {
            return new ClientLogoResponse(c.getId(), c.getName(), c.getLogoUrl(),
                    c.getLinkUrl(), c.getSortOrder());
        }
    }
}
