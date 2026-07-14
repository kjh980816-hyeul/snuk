package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.Campaign;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 홈/목록 노출용 공개 응답. 키·민감정보 없음. */
public record CampaignResponse(
        Long id,
        String title,
        String description,
        String gameName,
        String promoImageUrl,
        LocalDate eventDate,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        String status,
        String distributionType,
        String keyMode,
        int totalSlots,
        int filledSlots,
        boolean featured,
        Long ownerMemberId
) {
    public static CampaignResponse from(Campaign c) {
        return new CampaignResponse(
                c.getId(), c.getTitle(), c.getDescription(), c.getGameName(), c.getPromoImageUrl(),
                c.getEventDate(), c.getApplyStart(), c.getApplyEnd(),
                c.getStatus().name(), c.getDistributionType().name(), c.getKeyMode().name(),
                c.getTotalSlots(), c.getFilledSlots(), c.isFeatured(), c.getOwnerMemberId());
    }
}
