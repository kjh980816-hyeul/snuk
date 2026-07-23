package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.CampaignApplication;
import com.chzikon.member.domain.Member;

import java.time.LocalDateTime;

/** 어드민 신청자 목록(승인제 배정용). 닉네임/프사 포함(항목 14). */
public record ApplicationAdminView(
        Long applicationId,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String status,
        int followerSnapshot,
        Long assignedKeyId,
        LocalDateTime appliedAt,
        LocalDateTime decidedAt
) {
    public static ApplicationAdminView from(CampaignApplication a, Member m) {
        return new ApplicationAdminView(a.getId(), a.getMemberId(),
                m != null ? m.getNickname() : ("회원#" + a.getMemberId()),
                m != null ? m.getProfileImageUrl() : null,
                a.getStatus().name(),
                a.getFollowerSnapshot(), a.getAssignedKeyId(), a.getAppliedAt(), a.getDecidedAt());
    }
}
