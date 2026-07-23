package com.chzikon.streamer.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 게시판 글 신고(V15 항목 3). UNIQUE(post_id, reporter_id) = 1인 1신고. 처리=어드민이 글 삭제 또는 신고 기각. */
@Entity
@Table(name = "streamer_post_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StreamerPostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public StreamerPostReport(Long postId, Long reporterId, String reason) {
        this.postId = postId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
