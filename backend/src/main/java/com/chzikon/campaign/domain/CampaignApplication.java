package com.chzikon.campaign.domain;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 참가 신청. UNIQUE(campaign_id, member_id) = 1인 1신청(CMP-06). */
@Entity
@Table(name = "campaign_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CampaignApplication {

    /** 키 수령 후 후기 마감까지(일). */
    public static final int REVIEW_DUE_DAYS = 30;
    /** 마이페이지 연장 1회(일). */
    public static final int EXTENSION_DAYS = 7;

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "follower_snapshot", nullable = false)
    private int followerSnapshot;

    @Column(name = "assigned_key_id")
    private Long assignedKeyId;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 후기 마감(키 배정 시각 + 30일). 키 미배정이면 NULL. */
    @Column(name = "review_deadline")
    private LocalDateTime reviewDeadline;

    /** 마이페이지 7일 연장 사용 여부(게임당 1회). */
    @Column(name = "deadline_extended", nullable = false)
    private boolean deadlineExtended;

    /** 마감 경과+후기 미작성 경고 발생 시각. */
    @Column(name = "warned_at")
    private LocalDateTime warnedAt;

    public CampaignApplication(Long campaignId, Long memberId, int followerSnapshot, Status status) {
        this.campaignId = campaignId;
        this.memberId = memberId;
        this.followerSnapshot = followerSnapshot;
        this.status = status;
        this.appliedAt = LocalDateTime.now();
        if (status != Status.PENDING) {
            this.decidedAt = this.appliedAt;
        }
    }

    public void approve(Long assignedKeyId) {
        this.status = Status.APPROVED;
        this.assignedKeyId = assignedKeyId;
        this.decidedAt = LocalDateTime.now();
        if (assignedKeyId != null) {
            this.reviewDeadline = this.decidedAt.plusDays(REVIEW_DUE_DAYS);
        }
    }

    /** 마이페이지 후기 마감 연장 — 게임(캠페인)당 1회, 7일. */
    public void extendDeadline() {
        if (this.reviewDeadline == null || this.deadlineExtended) {
            throw new BusinessException(ErrorCode.DEADLINE_ALREADY_EXTENDED);
        }
        this.reviewDeadline = this.reviewDeadline.plusDays(EXTENSION_DAYS);
        this.deadlineExtended = true;
    }

    /** 마감 경과+후기 미작성 경고 기록(스윕에서 1회). */
    public void markWarned() {
        this.warnedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = Status.REJECTED;
        this.decidedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return this.status == Status.APPROVED;
    }
}
