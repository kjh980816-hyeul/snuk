package com.chzikon.collab.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 클라이언트 로고(협업 업체 그리드). 대표 업로드·순서(COL-04). */
@Entity
@Table(name = "client_logo")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientLogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200)
    private String name;

    @Column(name = "logo_url", nullable = false, length = 512)
    private String logoUrl;

    @Column(name = "link_url", length = 512)
    private String linkUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ClientLogo(String name, String logoUrl, String linkUrl, int sortOrder) {
        this.name = name;
        this.logoUrl = logoUrl;
        this.linkUrl = linkUrl;
        this.sortOrder = sortOrder;
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void update(String name, String logoUrl, String linkUrl, Integer sortOrder) {
        this.name = name;
        if (logoUrl != null) this.logoUrl = logoUrl;
        this.linkUrl = linkUrl;
        if (sortOrder != null) this.sortOrder = sortOrder;
        this.updatedAt = LocalDateTime.now();
    }
}
