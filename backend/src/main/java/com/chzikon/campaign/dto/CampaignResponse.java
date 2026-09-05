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
        Long ownerMemberId,
        java.util.List<com.chzikon.tournament.dto.ApplyFormJson.ApplyQuestion> applyQuestions,
        /** 서버 판정: 지금 신청 가능한가(OPEN). 프론트 버튼 노출은 이 값 기준. */
        boolean applyOpen,
        /** 서버 판정: 준비중(내용만 공개, 모집 정보·신청 숨김). */
        boolean preparing
) {
    public static CampaignResponse from(Campaign c) {
        return new CampaignResponse(
                c.getId(), c.getTitle(), c.getDescription(), c.getGameName(), c.getPromoImageUrl(),
                c.getEventDate(), c.getApplyStart(), c.getApplyEnd(),
                c.getStatus().name(), c.getDistributionType().name(), c.getKeyMode().name(),
                c.getTotalSlots(), c.getFilledSlots(), c.isFeatured(), c.getOwnerMemberId(),
                com.chzikon.tournament.dto.ApplyFormJson.questionsFromJson(c.getApplyQuestions()),
                c.isOpenForApply(), c.isPreparing());
    }
}
