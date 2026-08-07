package com.chzikon.community.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 커뮤니티 글 신고. UNIQUE(post_id, reporter_id) = 1인 1신고. 처리 = 어드민이 글 숨김·삭제 또는 신고 기각. */
@Entity
@Table(name = "community_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityReport {

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

    public CommunityReport(Long postId, Long reporterId, String reason) {
        this.postId = postId;
        this.reporterId = reporterId;
        this.reason = reason;
        this.createdAt = LocalDateTime.now();
    }
}
