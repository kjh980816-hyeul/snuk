package com.chzikon.ad.dto;

import com.chzikon.ad.domain.AdSlot;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AdSlotDtos {
    private AdSlotDtos() {
    }

    public record AdSlotRequest(
            @Size(max = 100) String title,
            @NotBlank @Size(max = 500) String imageUrl,
            @Size(max = 500) String linkUrl,
            Boolean active,
            Integer sortOrder,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        public boolean activeOrDefault() { return active == null || active; }
        public int sortOrderOrDefault() { return sortOrder == null ? 0 : sortOrder; }
    }

    /** 공개/어드민 공용 응답. live=지금 노출 중(서버 판정). */
    public record AdSlotResponse(
            Long id, String title, String imageUrl, String linkUrl, boolean active, int sortOrder,
            LocalDateTime startAt, LocalDateTime endAt, boolean live, LocalDateTime createdAt
    ) {
        public static AdSlotResponse from(AdSlot a) {
            return new AdSlotResponse(a.getId(), a.getTitle(), a.getImageUrl(), a.getLinkUrl(), a.isActive(),
                    a.getSortOrder(), a.getStartAt(), a.getEndAt(), a.isLiveAt(LocalDateTime.now()), a.getCreatedAt());
        }
    }
}
