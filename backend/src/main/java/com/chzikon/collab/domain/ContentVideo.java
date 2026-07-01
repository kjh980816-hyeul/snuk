package com.chzikon.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 콘텐츠 영상(히어로 대표/썸네일 리스트). 대표 CRUD(COL-01). */
@Entity
@Table(name = "content_video")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContentVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "video_url", nullable = false, length = 512)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "is_featured", nullable = false)
    private boolean featured;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ContentVideo(String title, String videoUrl, String thumbnailUrl,
                        boolean featured, int sortOrder) {
        this.title = title;
        this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.featured = featured;
        this.sortOrder = sortOrder;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String title, String videoUrl, String thumbnailUrl,
                       Boolean featured, Integer sortOrder) {
        if (title != null) this.title = title;
        if (videoUrl != null) this.videoUrl = videoUrl;
        this.thumbnailUrl = thumbnailUrl;
        if (featured != null) this.featured = featured;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }
}
