package com.chzikon.spotlight.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 방송 홍보(스포트라이트). 승인제 — 어드민 승인 시각부터 2시간 노출, 최대 2개 표시. */
@Entity
@Table(name = "spotlight")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Spotlight {

    public static final int EXPOSURE_HOURS = 2;

    public enum Platform { CHZZK, SOOP, YOUTUBE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Platform platform;

    @Column(name = "stream_url", nullable = false, length = 512)
    private String streamUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** 승인제(어드민). 승인 전에는 사이드바 미노출. */
    @Column(name = "is_approved", nullable = false)
    private boolean approved;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /** 방송 예정 일시(선택, V15). */
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    public Spotlight(Long memberId, String title, Platform platform, String streamUrl, LocalDateTime scheduledAt) {
        this.memberId = memberId;
        this.title = title;
        this.platform = platform;
        this.streamUrl = streamUrl;
        this.scheduledAt = scheduledAt;
        this.createdAt = LocalDateTime.now();
        // 예정 일시가 있으면 그 시각부터 2시간, 없으면 등록 시각부터 2시간
        this.expiresAt = (scheduledAt != null ? scheduledAt : this.createdAt).plusHours(EXPOSURE_HOURS);
    }

    /**
     * 승인 — 예정 일시가 있으면 [예정 일시, +2시간] 창에서 자동 노출·자동 종료.
     * 예정 없으면 승인 시각부터 2시간.
     */
    public void approve() {
        this.approved = true;
        this.approvedAt = LocalDateTime.now();
        this.expiresAt = (this.scheduledAt != null ? this.scheduledAt : this.approvedAt).plusHours(EXPOSURE_HOURS);
    }
}
