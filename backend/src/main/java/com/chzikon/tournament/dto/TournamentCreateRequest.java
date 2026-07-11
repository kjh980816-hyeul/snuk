package com.chzikon.tournament.dto;

import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.domain.TournamentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TournamentCreateRequest(
        @NotBlank String title,
        String description,
        String gameName,
        String bannerImageUrl,
        String detailImageUrl,
        LocalDate eventDate,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        @PositiveOrZero int capacity,
        TournamentStatus status,
        String resultText,
        boolean featured,
        int sortOrder
) {
    public Tournament toEntity() {
        return Tournament.builder()
                .title(title)
                .description(description)
                .gameName(gameName)
                .bannerImageUrl(bannerImageUrl)
                .detailImageUrl(detailImageUrl)
                .eventDate(eventDate)
                .applyStart(applyStart)
                .applyEnd(applyEnd)
                .capacity(capacity)
                .status(status)
                .resultText(resultText)
                .featured(featured)
                .sortOrder(sortOrder)
                .build();
    }
}
