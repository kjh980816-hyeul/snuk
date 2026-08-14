package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 페이지 공지 (V23, 본인 관리). */
@Entity
@Table(name = "streamer_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean important;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StreamerNotice(Long streamerId, String title, String body, boolean important) {
        this.streamerId = streamerId;
        this.title = title;
        this.body = body;
        this.important = important;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String body, Boolean important) {
        if (title != null) this.title = title;
        this.body = body;
        if (important != null) this.important = important;
        this.updatedAt = LocalDateTime.now();
    }
}
