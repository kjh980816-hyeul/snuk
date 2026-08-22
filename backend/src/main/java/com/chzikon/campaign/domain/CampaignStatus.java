package com.chzikon.campaign.domain;

/** 캠페인 상태머신 (CMP-02): SCHEDULED → OPEN → ONGOING → CLOSED. ONGOING=모집 마감 후 컨텐츠 진행 중(신청 불가). */
public enum CampaignStatus {
    SCHEDULED,
    OPEN,
    ONGOING,
    CLOSED
}
