package com.chzikon.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 감사 로그. 어드민 행위(행위자·시각·대상·전후 요약) 기록(ADM-02). */
@Entity
@Table(name = "admin_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdminLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_member_id")
    private Long actorMemberId;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "target_type", length = 60)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(columnDefinition = "TEXT")
    private String detail;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AdminLog(Long actorMemberId, String action, String targetType, Long targetId, String detail) {
        this.actorMemberId = actorMemberId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }
}
