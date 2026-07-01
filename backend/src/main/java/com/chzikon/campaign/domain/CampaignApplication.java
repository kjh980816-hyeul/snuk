package com.chzikon.campaign.domain;

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
    }

    public void reject() {
        this.status = Status.REJECTED;
        this.decidedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return this.status == Status.APPROVED;
    }
}
