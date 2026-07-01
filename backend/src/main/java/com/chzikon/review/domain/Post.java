package com.chzikon.review.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 게시글(후기 포함). category=REVIEW + campaign_id 연결(REV-02). */
@Entity
@Table(name = "post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category = PostCategory.REVIEW;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_hidden", nullable = false)
    private boolean hidden;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Post(PostCategory category, Long campaignId, Long memberId, String title, String content) {
        this.category = category;
        this.campaignId = campaignId;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void edit(String title, String content) {
        if (title != null) this.title = title;
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isOwnedBy(Long memberId) {
        return this.memberId.equals(memberId);
    }
}
