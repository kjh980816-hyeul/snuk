package com.chzikon.tournament.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.tournament.domain.TournamentParticipant;

import java.time.LocalDateTime;

/** 대회 주최자(소유 스트리머/ADMIN) 참가자 관리 목록 — 닉네임·프사 포함. */
public record ParticipantManageView(
        Long participantId,
        Long memberId,
        String nickname,
        String profileImageUrl,
        int followerSnapshot,
        String status,
        LocalDateTime appliedAt
) {
    public static ParticipantManageView of(TournamentParticipant p, Member m) {
        return new ParticipantManageView(
                p.getId(), p.getMemberId(),
                m != null ? m.getNickname() : "(탈퇴 회원)",
                m != null ? m.getProfileImageUrl() : null,
                p.getFollowerSnapshot(), p.getStatus().name(), p.getAppliedAt());
    }
}
