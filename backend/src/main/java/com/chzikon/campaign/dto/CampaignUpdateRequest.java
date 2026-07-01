package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 부분 수정 허용(null 은 미변경 의도지만 일부 필드는 그대로 set — 서비스에서 처리). */
public record CampaignUpdateRequest(
        String title,
        String description,
        String gameName,
        String promoImageUrl,
        LocalDate eventDate,
        LocalDateTime applyStart,
        LocalDateTime applyEnd,
        CampaignStatus status,
        DistributionType distributionType,
        KeyMode keyMode,
        Integer totalSlots,
        Boolean featured,
        Integer sortOrder
) {
}
