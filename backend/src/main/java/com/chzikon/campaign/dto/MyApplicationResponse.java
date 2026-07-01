package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.CampaignApplication;

import java.time.LocalDateTime;

/**
 * 내 신청 상태. assignedKey 는 본인에게만, 마스킹 해제(reveal=true)는 본인 확인 시점에만 채워짐.
 */
public record MyApplicationResponse(
        Long applicationId,
        Long campaignId,
        String status,
        boolean hasAssignedKey,
        String assignedKey,   // null 또는 마스킹/평문(본인 reveal 시)
        LocalDateTime appliedAt
) {
    public static MyApplicationResponse masked(CampaignApplication app, boolean hasKey, String maskedOrNull) {
        return new MyApplicationResponse(app.getId(), app.getCampaignId(), app.getStatus().name(),
                hasKey, maskedOrNull, app.getAppliedAt());
    }
}
