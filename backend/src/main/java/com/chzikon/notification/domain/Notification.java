package com.chzikon.notification.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 회원 알림함 (V23). 이벤트 발생 시 서버가 적재, 회원이 읽음 처리. */
@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** APPLICATION_APPROVED / APPLICATION_REJECTED / TOURNAMENT_APPROVED / TOURNAMENT_REJECTED / ROLE_APPROVED / ROLE_REJECTED / NOTICE */
    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @Column(name = "link_path", length = 200)
    private String linkPath;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Notification(Long memberId, String type, String title, String body, String linkPath) {
        this.memberId = memberId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkPath = linkPath;
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isUnread() {
        return this.readAt == null;
    }
}
