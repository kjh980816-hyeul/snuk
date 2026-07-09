package com.chzikon.tournament.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.tournament.domain.TournamentParticipant;

/** 공개 참가자 로스터(승인된 참가자만) — 대진표/로스터 표시용. */
public record ParticipantPublicView(
        Long participantId,
        String nickname,
        String profileImageUrl,
        String provider
) {
    public static ParticipantPublicView of(TournamentParticipant p, Member m) {
        return new ParticipantPublicView(p.getId(),
                m != null ? m.getNickname() : "참가자",
                m != null ? m.getProfileImageUrl() : null,
                m != null ? m.getProvider().name() : "CHZZK");
    }
}
