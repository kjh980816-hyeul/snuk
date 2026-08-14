package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 방송도우미 노래 신청 (V23). 시청자 신청 → 스트리머가 재생/스킵 처리. */
@Entity
@Table(name = "streamer_song_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerSongRequest {

    public enum Status { QUEUED, PLAYED, SKIPPED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(nullable = false, length = 200)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.QUEUED;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public StreamerSongRequest(Long streamerId, Long requesterId, String title) {
        this.streamerId = streamerId;
        this.requesterId = requesterId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
    }

    public void decide(Status status) {
        this.status = status;
        this.decidedAt = LocalDateTime.now();
    }

    public boolean isQueued() {
        return this.status == Status.QUEUED;
    }
}
