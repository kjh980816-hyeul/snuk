package com.chzikon.crew;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 크루 페이지 콘텐츠(DATA JSON, V25) — /crew/<slug>/ 정적 HTML이 로드 시 조회. */
@Entity
@Table(name = "crew_page")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewPage {

    @Id
    @Column(length = 100)
    private String slug;

    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String data;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CrewPage(String slug, String data, Long updatedBy) {
        this.slug = slug;
        this.data = data;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String data, Long updatedBy) {
        this.data = data;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
