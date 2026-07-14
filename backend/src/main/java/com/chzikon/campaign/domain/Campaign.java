package com.chzikon.campaign.domain;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "game_name", length = 200)
    private String gameName;

    @Column(name = "promo_image_url", length = 512)
    private String promoImageUrl;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "apply_start")
    private LocalDateTime applyStart;

    @Column(name = "apply_end")
    private LocalDateTime applyEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatus status = CampaignStatus.SCHEDULED;

    @Enumerated(EnumType.STRING)
    @Column(name = "distribution_type", nullable = false, length = 20)
    private DistributionType distributionType = DistributionType.FCFS;

    @Enumerated(EnumType.STRING)
    @Column(name = "key_mode", nullable = false, length = 20)
    private KeyMode keyMode = KeyMode.QUANTITY;

    @Column(name = "total_slots", nullable = false)
    private int totalSlots;

    @Column(name = "filled_slots", nullable = false)
    private int filledSlots;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 등록 스트리머(NULL = 어드민 등록). 본인 수정/삭제 권한 판별용. */
    @Column(name = "owner_member_id")
    private Long ownerMemberId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Campaign(String title, String description, String gameName, String promoImageUrl,
                     LocalDate eventDate, LocalDateTime applyStart, LocalDateTime applyEnd,
                     CampaignStatus status, DistributionType distributionType, KeyMode keyMode,
                     int totalSlots, boolean featured, int sortOrder) {
        this.title = title;
        this.description = description;
        this.gameName = gameName;
        this.promoImageUrl = promoImageUrl;
        this.eventDate = eventDate;
        this.applyStart = applyStart;
        this.applyEnd = applyEnd;
        this.status = status != null ? status : CampaignStatus.SCHEDULED;
        this.distributionType = distributionType != null ? distributionType : DistributionType.FCFS;
        this.keyMode = keyMode != null ? keyMode : KeyMode.QUANTITY;
        this.totalSlots = totalSlots;
        this.filledSlots = 0;
        this.featured = featured;
        this.sortOrder = sortOrder;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, String gameName, String promoImageUrl,
                       LocalDate eventDate, LocalDateTime applyStart, LocalDateTime applyEnd,
                       CampaignStatus status, DistributionType distributionType, KeyMode keyMode,
                       Integer totalSlots, Boolean featured, Integer sortOrder) {
        if (title != null) this.title = title;
        this.description = description;
        this.gameName = gameName;
        this.promoImageUrl = promoImageUrl;
        this.eventDate = eventDate;
        this.applyStart = applyStart;
        this.applyEnd = applyEnd;
        if (status != null) this.status = status;
        if (distributionType != null) this.distributionType = distributionType;
        if (keyMode != null) this.keyMode = keyMode;
        if (totalSlots != null) this.totalSlots = totalSlots;
        if (featured != null) this.featured = featured;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    /** 스트리머 본인 등록 컨텐츠 표시. */
    public void assignOwner(Long memberId) {
        this.ownerMemberId = memberId;
    }

    public boolean isOwnedBy(Long memberId) {
        return this.ownerMemberId != null && this.ownerMemberId.equals(memberId);
    }

    public boolean isOpenForApply() {
        return this.status == CampaignStatus.OPEN;
    }

    public boolean hasFreeSlot() {
        return this.filledSlots < this.totalSlots;
    }

    /** 슬롯 1개 차감(원자 처리는 비관적 락 트랜잭션 내에서 호출). 소진 시 예외. */
    public void fillOneSlot() {
        if (!hasFreeSlot()) {
            throw new BusinessException(ErrorCode.CAMPAIGN_FULL);
        }
        this.filledSlots += 1;
        this.updatedAt = LocalDateTime.now();
    }

    /** 배정 취소 시 슬롯 환원. */
    public void releaseOneSlot() {
        if (this.filledSlots > 0) {
            this.filledSlots -= 1;
            this.updatedAt = LocalDateTime.now();
        }
    }
}
