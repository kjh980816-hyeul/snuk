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
        boolean featured,
        int sortOrder
) {
    public static TournamentResponse from(Tournament t) {
        return new TournamentResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getGameName(), t.getBannerImageUrl(), t.getDetailImageUrl(),
                t.getEventDate(), t.getApplyStart(), t.getApplyEnd(),
                t.getCapacity(), t.getFilledSlots(), t.getStatus().name(),
                t.getResultText(), t.isFeatured(), t.getSortOrder());
    }
}
