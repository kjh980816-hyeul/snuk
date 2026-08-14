package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 위키 문서 (V23, 본인 작성). sections = JSON [{"t":"제목","b":"본문"}]. */
@Entity
@Table(name = "streamer_wiki")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerWiki {

    @Id
    @Column(name = "streamer_id")
    private Long streamerId;

    @Column(columnDefinition = "TEXT")
    private String sections;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public StreamerWiki(Long streamerId, String sections) {
        this.streamerId = streamerId;
        this.sections = sections;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String sections) {
        this.sections = sections;
        this.updatedAt = LocalDateTime.now();
    }
}
