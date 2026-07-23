package com.chzikon.resource;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 무료소스 자료(V15 항목 19) — 어드민 업로드, 공개 다운로드. */
@Entity
@Table(name = "free_resource")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FreeResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "file_url", length = 512)
    private String fileUrl;

    /** 목록 썸네일(선택). */
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "uploader_id", nullable = false)
    private Long uploaderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public FreeResource(String title, String description, String fileUrl, String imageUrl, Long uploaderId) {
        this.title = title;
        this.description = description;
        this.fileUrl = fileUrl;
        this.imageUrl = imageUrl;
        this.uploaderId = uploaderId;
        this.createdAt = LocalDateTime.now();
    }

    public void update(String title, String description, String fileUrl, String imageUrl) {
        if (title != null && !title.isBlank()) this.title = title;
        this.description = description;
        if (fileUrl != null) this.fileUrl = fileUrl;
        this.imageUrl = imageUrl;
    }
}
