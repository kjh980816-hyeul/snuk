package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 방송 일정 (V23, 본인 관리). */
@Entity
@Table(name = "streamer_schedule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 100)
    private String game;

    /** 함께하는 스트리머(자유 표기, 쉼표 구분). */
    @Column(length = 300)
    private String mates;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public StreamerSchedule(Long streamerId, LocalDateTime startAt, String title, String game, String mates) {
        this.streamerId = streamerId;
        this.startAt = startAt;
        this.title = title;
        this.game = game;
        this.mates = mates;
        this.createdAt = LocalDateTime.now();
    }

    public void update(LocalDateTime startAt, String title, String game, String mates) {
        if (startAt != null) this.startAt = startAt;
        if (title != null) this.title = title;
        this.game = game;
        this.mates = mates;
    }
}
