package com.chzikon.ad.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 홈 상단 광고 슬롯 — 이미지+링크. 노출 on/off + 선택적 기간. */
@Entity
@Table(name = "ad_slot")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String title;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AdSlot(String title, String imageUrl, String linkUrl, boolean active, int sortOrder,
                  LocalDateTime startAt, LocalDateTime endAt, Long createdBy) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.active = active;
        this.sortOrder = sortOrder;
        this.startAt = startAt;
        this.endAt = endAt;
        this.createdBy = createdBy;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String imageUrl, String linkUrl, boolean active, int sortOrder,
                       LocalDateTime startAt, LocalDateTime endAt) {
        this.title = title;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.active = active;
        this.sortOrder = sortOrder;
        this.startAt = startAt;
        this.endAt = endAt;
        this.updatedAt = LocalDateTime.now();
    }

    /** 지금 노출 가능한가 — 활성 + 기간(있으면) 안. 서버 판정값(응답 live). */
    public boolean isLiveAt(LocalDateTime now) {
        if (!active) return false;
        if (startAt != null && now.isBefore(startAt)) return false;
        if (endAt != null && now.isAfter(endAt)) return false;
        return true;
    }
}
