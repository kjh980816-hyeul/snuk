package com.chzikon.tournament.dto;

import com.chzikon.tournament.domain.TournamentParticipant;

import java.time.LocalDateTime;

/** 내 참가 신청 상태 — 본인만 조회. */
public record MyParticipationResponse(
        Long participantId,
        Long tournamentId,
        String status,
        LocalDateTime appliedAt
) {
    public static MyParticipationResponse from(TournamentParticipant p) {
        return new MyParticipationResponse(p.getId(), p.getTournamentId(), p.getStatus().name(), p.getAppliedAt());
    }
}
