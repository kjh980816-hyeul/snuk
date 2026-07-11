package com.chzikon.tournament.domain;

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
@Table(name = "tournament")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "game_name", length = 200)
    private String gameName;

    @Column(name = "banner_image_url", length = 512)
    private String bannerImageUrl;

    @Column(name = "detail_image_url", length = 512)
    private String detailImageUrl;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "apply_start")
    private LocalDateTime applyStart;

    @Column(name = "apply_end")
    private LocalDateTime applyEnd;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "filled_slots", nullable = false)
    private int filledSlots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentStatus status = TournamentStatus.SCHEDULED;

    @Column(name = "result_text", columnDefinition = "TEXT")
    private String resultText;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Tournament(String title, String description, String gameName, String bannerImageUrl,
                       String detailImageUrl,
                       LocalDate eventDate, LocalDateTime applyStart, LocalDateTime applyEnd,
                       int capacity, TournamentStatus status, String resultText,
                       boolean featured, int sortOrder) {
        this.title = title;
        this.description = description;
        this.gameName = gameName;
        this.bannerImageUrl = bannerImageUrl;
        this.detailImageUrl = detailImageUrl;
        this.eventDate = eventDate;
        this.applyStart = applyStart;
        this.applyEnd = applyEnd;
        this.capacity = capacity;
        this.filledSlots = 0;
        this.status = status != null ? status : TournamentStatus.SCHEDULED;
        this.resultText = resultText;
        this.featured = featured;
        this.sortOrder = sortOrder;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String description, String gameName, String bannerImageUrl,
                       String detailImageUrl,
                       LocalDate eventDate, LocalDateTime applyStart, LocalDateTime applyEnd,
                       Integer capacity, TournamentStatus status, String resultText,
                       Boolean featured, Integer sortOrder) {
        if (title != null) this.title = title;
        this.description = description;
        this.gameName = gameName;
        this.bannerImageUrl = bannerImageUrl;
        this.detailImageUrl = detailImageUrl;
        this.eventDate = eventDate;
        this.applyStart = applyStart;
        this.applyEnd = applyEnd;
        if (capacity != null) this.capacity = capacity;
        if (status != null) this.status = status;
        this.resultText = resultText;
        if (featured != null) this.featured = featured;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOpenForApply() {
        return this.status == TournamentStatus.OPEN;
    }

    public boolean hasFreeSlot() {
        return this.filledSlots < this.capacity;
    }

    /** 승인 시 정원 1 차감(비관적 락 트랜잭션 내에서 호출). 정원 초과 시 예외. */
    public void fillOneSlot() {
        if (!hasFreeSlot()) {
            throw new BusinessException(ErrorCode.TOURNAMENT_FULL);
        }
        this.filledSlots += 1;
        this.updatedAt = LocalDateTime.now();
    }
}
