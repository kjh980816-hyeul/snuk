package com.chzikon.campaign.domain;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 게임 키 (UNIQUE_KEY 모드 전용). key_value_enc 는 암호문(평문 금지). */
@Entity
@Table(name = "game_key")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameKey {

    public enum Status { AVAILABLE, ASSIGNED, REVOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "key_value_enc", nullable = false, length = 1024)
    private String keyValueEnc;

    @Column(name = "key_fingerprint", nullable = false, length = 64)
    private String keyFingerprint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.AVAILABLE;

    @Column(name = "assigned_member_id")
    private Long assignedMemberId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public GameKey(Long campaignId, String keyValueEnc, String keyFingerprint) {
        this.campaignId = campaignId;
        this.keyValueEnc = keyValueEnc;
        this.keyFingerprint = keyFingerprint;
        this.status = Status.AVAILABLE;
        this.createdAt = LocalDateTime.now();
    }

    public void assignTo(Long memberId) {
        if (this.status != Status.AVAILABLE) {
            throw new BusinessException(ErrorCode.KEY_ALREADY_ASSIGNED);
        }
        this.status = Status.ASSIGNED;
        this.assignedMemberId = memberId;
        this.assignedAt = LocalDateTime.now();
    }

    /** 배정된 키 무효화(재배정 동선). */
    public void revoke() {
        this.status = Status.REVOKED;
    }

    public boolean isAvailable() {
        return this.status == Status.AVAILABLE;
    }
}
