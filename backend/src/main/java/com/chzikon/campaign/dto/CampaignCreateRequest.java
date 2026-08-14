package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CampaignCreateRequest(
        @NotBlank String title,
        String description,
        String gameName,
        String promoImageUrl,
        LocalDate eventDate,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        CampaignStatus status,
        DistributionType distributionType,
        KeyMode keyMode,
        @PositiveOrZero int totalSlots,
        boolean featured,
        int sortOrder,
        java.util.List<com.chzikon.tournament.dto.ApplyFormJson.ApplyQuestion> applyQuestions
) {
    public Campaign toEntity() {
        return Campaign.builder()
                .title(title)
                .description(description)
                .gameName(gameName)
                .promoImageUrl(promoImageUrl)
                .eventDate(eventDate)
                .applyStart(applyStart)
                .applyEnd(applyEnd)
                .status(status)
                .distributionType(distributionType)
                .keyMode(keyMode)
                .totalSlots(totalSlots)
                .featured(featured)
                .sortOrder(sortOrder)
                .applyQuestions(com.chzikon.tournament.dto.ApplyFormJson.questionsToJson(applyQuestions))
                .build();
    }
}
