package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.CampaignApplication;

import java.time.LocalDateTime;

/** 어드민 신청자 목록(승인제 배정용). */
public record ApplicationAdminView(
        Long applicationId,
        Long memberId,
        String status,
        int followerSnapshot,
        Long assignedKeyId,
        LocalDateTime appliedAt,
        LocalDateTime decidedAt
) {
    public static ApplicationAdminView from(CampaignApplication a) {
        return new ApplicationAdminView(a.getId(), a.getMemberId(), a.getStatus().name(),
                a.getFollowerSnapshot(), a.getAssignedKeyId(), a.getAppliedAt(), a.getDecidedAt());
    }
}
