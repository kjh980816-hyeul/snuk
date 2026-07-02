package com.chzikon.tournament.dto;

import com.chzikon.tournament.domain.TournamentParticipant;

import java.time.LocalDateTime;

/** 어드민 참가 신청자 목록(승인/거절용). */
public record ParticipantAdminView(
        Long participantId,
        Long memberId,
        String status,
        int followerSnapshot,
        LocalDateTime appliedAt,
        LocalDateTime decidedAt
) {
    public static ParticipantAdminView from(TournamentParticipant p) {
        return new ParticipantAdminView(p.getId(), p.getMemberId(), p.getStatus().name(),
                p.getFollowerSnapshot(), p.getAppliedAt(), p.getDecidedAt());
    }
}
