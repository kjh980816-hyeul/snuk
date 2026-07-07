package com.chzikon.member.dto;

import com.chzikon.member.domain.Member;

import java.time.LocalDateTime;

/** 어드민 회원 관리 목록용 요약. */
public record MemberSummary(
        Long id,
        String provider,
        String channelId,
        String nickname,
        String profileImageUrl,
        Integer followerCount,
        String role,
        boolean roleOverridden,
        LocalDateTime createdAt
) {
    public static MemberSummary from(Member m) {
        return new MemberSummary(m.getId(), m.getProvider().name(), m.getChannelId(), m.getNickname(),
                m.getProfileImageUrl(), m.getFollowerCount(), m.getRole().name(),
                m.isRoleOverridden(), m.getCreatedAt());
    }
}
