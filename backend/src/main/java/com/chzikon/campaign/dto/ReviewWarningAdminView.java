package com.chzikon.campaign.dto;

import java.time.LocalDateTime;

/** 후기 미작성 경고 로그(어드민) — 키 수령 후 마감 경과·미작성 건. */
public record ReviewWarningAdminView(
        Long applicationId,
        Long memberId,
        String nickname,
        Long campaignId,
        String campaignTitle,
        LocalDateTime reviewDeadline,
        LocalDateTime warnedAt,
        boolean deadlineExtended,
        boolean reviewWritten
) {
}
