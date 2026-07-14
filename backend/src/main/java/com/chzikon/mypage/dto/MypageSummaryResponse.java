package com.chzikon.mypage.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 마이페이지 집계 응답(MY-01) — 새 테이블 없이 기존 도메인 조회만.
 * 배정 키는 항상 마스킹(평문 reveal 은 캠페인 상세의 my-application 에서만).
 */
public record MypageSummaryResponse(
        List<MyCampaignItem> applications,
        List<MyTournamentItem> tournaments,
        List<MyReviewItem> reviews,
        List<MyOrderItem> orders
) {
    public record MyCampaignItem(
            Long applicationId,
            Long campaignId,
            String campaignTitle,
            String status,
            boolean hasAssignedKey,
            String maskedKey,
            LocalDateTime appliedAt,
            LocalDateTime reviewDeadline,   // 키 수령 후 후기 마감(없으면 null)
            boolean deadlineExtended,       // 7일 연장 사용 여부(게임당 1회)
            boolean warned,                 // 마감 경과 경고 여부
            boolean reviewWritten           // 후기 작성 완료 여부
    ) {}

    public record MyTournamentItem(
            Long participantId,
            Long tournamentId,
            String tournamentTitle,
            String status,
            LocalDateTime appliedAt
    ) {}

    public record MyReviewItem(
            Long postId,
            Long campaignId,
            String title,
            boolean hidden,
            LocalDateTime createdAt
    ) {}

    public record MyOrderItem(
            Long orderId,
            String goodsName,
            int quantity,
            int totalAmount,
            String status,
            LocalDateTime createdAt
    ) {}
}
