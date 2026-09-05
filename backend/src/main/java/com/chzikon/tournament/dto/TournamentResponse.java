package com.chzikon.tournament.dto;

import com.chzikon.tournament.domain.Tournament;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 대회 목록/상세 공개 응답. */
public record TournamentResponse(
        Long id,
        String title,
        String description,
        String gameName,
        String bannerImageUrl,
        String detailImageUrl,
        LocalDate eventDate,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        int capacity,
        int filledSlots,
        String status,
        String resultText,
        java.util.List<ApplyFormJson.ApplyQuestion> applyQuestions,
        boolean featured,
        int sortOrder,
        Long ownerMemberId,
        /** 서버 판정: 지금 참가 신청 가능한가(OPEN). */
        boolean applyOpen,
        /** 서버 판정: 준비중(내용만 공개, 참가 정보·신청 숨김). */
        boolean preparing
) {
    public static TournamentResponse from(Tournament t) {
        return new TournamentResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getGameName(), t.getBannerImageUrl(), t.getDetailImageUrl(),
                t.getEventDate(), t.getApplyStart(), t.getApplyEnd(),
                t.getCapacity(), t.getFilledSlots(), t.getStatus().name(),
                t.getResultText(), ApplyFormJson.questionsFromJson(t.getApplyQuestions()),
                t.isFeatured(), t.getSortOrder(), t.getOwnerMemberId(),
                t.isOpenForApply(), t.isPreparing());
    }
}
