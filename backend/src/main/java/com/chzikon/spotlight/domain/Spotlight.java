package com.chzikon.spotlight.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 방송 홍보(스포트라이트). 등록 후 2시간 노출, 사이드바 최대 2개 표시. */
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

    public Spotlight(Long memberId, String title, Platform platform, String streamUrl) {
        this.memberId = memberId;
        this.title = title;
        this.platform = platform;
        this.streamUrl = streamUrl;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = this.createdAt.plusHours(EXPOSURE_HOURS);
    }
}
