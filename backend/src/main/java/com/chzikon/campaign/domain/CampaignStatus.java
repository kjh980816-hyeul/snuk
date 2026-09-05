package com.chzikon.campaign.domain;

/**
 * 캠페인 상태머신 (CMP-02): PREPARING → SCHEDULED → OPEN → ONGOING → CLOSED.
 * <ul>
 *   <li>PREPARING = 준비중. 내용(제목·소개·이미지)만 공개, 모집 정보·신청 불가.</li>
 *   <li>SCHEDULED = 오픈예정. 신청 기간이 잡혀 있고 아직 오픈 전.</li>
 *   <li>OPEN = 모집중(유일하게 신청 가능).</li>
 *   <li>ONGOING = 모집 마감 후 컨텐츠 진행 중(신청 불가).</li>
 *   <li>CLOSED = 종료.</li>
 * </ul>
 */
public enum CampaignStatus {
    PREPARING,
    SCHEDULED,
    OPEN,
    ONGOING,
    CLOSED
}
