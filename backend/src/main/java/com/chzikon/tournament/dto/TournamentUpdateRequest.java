package com.chzikon.tournament.dto;

import com.chzikon.tournament.domain.TournamentStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TournamentUpdateRequest(
        String title,
        String description,
        String gameName,
        String bannerImageUrl,
        String detailImageUrl,
        LocalDate eventDate,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        Integer capacity,
        TournamentStatus status,
        String resultText,
        Boolean featured,
        Integer sortOrder
) {
}
