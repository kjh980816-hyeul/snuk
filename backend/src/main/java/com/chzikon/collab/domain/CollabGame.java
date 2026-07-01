package com.chzikon.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 콜라보 게임 카드(게임링크/후기링크). 대표 CRUD + 정렬(COL-02). */
@Entity
@Table(name = "collab_game")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollabGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 512)
    private String thumbnailUrl;

    @Column(name = "game_link_url", length = 512)
    private String gameLinkUrl;

    @Column(name = "review_link_url", length = 512)
    private String reviewLinkUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CollabGame(String name, String description, String thumbnailUrl,
                      String gameLinkUrl, String reviewLinkUrl, int sortOrder) {
        this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.gameLinkUrl = gameLinkUrl;
        this.reviewLinkUrl = reviewLinkUrl;
        this.sortOrder = sortOrder;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String description, String thumbnailUrl,
                       String gameLinkUrl, String reviewLinkUrl, Integer sortOrder) {
        if (name != null) this.name = name;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.gameLinkUrl = gameLinkUrl;
        this.reviewLinkUrl = reviewLinkUrl;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }
}
