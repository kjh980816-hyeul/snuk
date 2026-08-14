package com.chzikon.grant.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 스트리머 권한 신청 (V23, 데모 '권한 신청' 메뉴). VIEWER 가 신청 → 어드민 승인 시 STREAMER 오버라이드. */
@Entity
@Table(name = "role_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleRequest {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(length = 1000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    public RoleRequest(Long memberId, String message) {
        this.memberId = memberId;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public void approve(Long decidedBy) {
        this.status = Status.APPROVED;
        this.decidedBy = decidedBy;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject(Long decidedBy) {
        this.status = Status.REJECTED;
        this.decidedBy = decidedBy;
        this.decidedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.status == Status.PENDING;
    }
}
