package com.chzikon.tournament.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.tournament.domain.TournamentParticipant;

import java.time.LocalDateTime;
import java.util.List;

/** 어드민 참가 신청자 목록(승인/거절용). 닉네임(항목 14)·질문 답변(항목 17: 텍스트+사진) 포함. */
public record ParticipantAdminView(
        Long participantId,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String status,
        int followerSnapshot,
        LocalDateTime appliedAt,
        LocalDateTime decidedAt,
        List<ApplyFormJson.ApplyAnswer> answers
) {
    public static ParticipantAdminView of(TournamentParticipant p, Member m) {
        return new ParticipantAdminView(p.getId(), p.getMemberId(),
                m != null ? m.getNickname() : ("회원#" + p.getMemberId()),
                m != null ? m.getProfileImageUrl() : null,
                p.getStatus().name(),
                p.getFollowerSnapshot(), p.getAppliedAt(), p.getDecidedAt(),
                ApplyFormJson.answersFromJson(p.getAnswers()));
    }
}
