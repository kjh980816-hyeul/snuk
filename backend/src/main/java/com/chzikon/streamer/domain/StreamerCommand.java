package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 방송도우미 채팅 명령어 (V23, 본인 관리). UNIQUE(streamer_id, name). */
@Entity
@Table(name = "streamer_command")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "streamer_id", nullable = false)
    private Long streamerId;

    /** 예: !인사, !디스코드 */
    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 500)
    private String response;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public StreamerCommand(Long streamerId, String name, String response, boolean enabled, int sortOrder) {
        this.streamerId = streamerId;
        this.name = name;
        this.response = response;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String name, String response, Boolean enabled, Integer sortOrder) {
        if (name != null) this.name = name;
        if (response != null) this.response = response;
        if (enabled != null) this.enabled = enabled;
        if (sortOrder != null) this.sortOrder = sortOrder;
    }
}
