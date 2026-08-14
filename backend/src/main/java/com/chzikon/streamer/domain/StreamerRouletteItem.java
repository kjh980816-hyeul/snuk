package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 방송도우미 후원 룰렛 항목 (V23, 본인 관리). 추첨은 프론트(가중치 랜덤). */
@Entity
@Table(name = "streamer_roulette_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerRouletteItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false)
    private int weight = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public StreamerRouletteItem(Long streamerId, String label, int weight, int sortOrder) {
        this.streamerId = streamerId;
        this.label = label;
        this.weight = Math.max(1, weight);
        this.sortOrder = sortOrder;
    }

    public void update(String label, Integer weight, Integer sortOrder) {
        if (label != null) this.label = label;
        if (weight != null) this.weight = Math.max(1, weight);
        if (sortOrder != null) this.sortOrder = sortOrder;
    }
}
