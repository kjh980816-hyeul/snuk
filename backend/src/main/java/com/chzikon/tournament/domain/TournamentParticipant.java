package com.chzikon.tournament.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 대회 참가 신청. UNIQUE(tournament_id, member_id) = 1인 1신청. 승인/거절은 어드민. */
@Entity
@Table(name = "tournament_participant")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TournamentParticipant {

    public enum Status { PENDING, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tournament_id", nullable = false)
    private Long tournamentId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "follower_snapshot", nullable = false)
    private int followerSnapshot;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /** 참가 질문 답변(JSON 배열 문자열 — V15 항목 17). */
    @Column(columnDefinition = "TEXT")
    private String answers;

    public TournamentParticipant(Long tournamentId, Long memberId, int followerSnapshot, String answers) {
        this.tournamentId = tournamentId;
        this.memberId = memberId;
        this.followerSnapshot = followerSnapshot;
        this.answers = answers;
        this.status = Status.PENDING;
        this.appliedAt = LocalDateTime.now();
    }

    public void approve() {
        this.status = Status.APPROVED;
        this.decidedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = Status.REJECTED;
        this.decidedAt = LocalDateTime.now();
    }

    public boolean isApproved() {
        return this.status == Status.APPROVED;
    }
}
