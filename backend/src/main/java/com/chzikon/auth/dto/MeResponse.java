package com.chzikon.auth.dto;

import com.chzikon.member.domain.Member;

public record MeResponse(
        Long id,
        String nickname,
        String profileImageUrl,
        Integer followerCount,
        String role,
        boolean profileImageOverridden,
        int points
) {
    public static MeResponse from(Member m) {
        return new MeResponse(m.getId(), m.getNickname(), m.getProfileImageUrl(),
                m.getFollowerCount(), m.getRole().name(), m.isProfileImageOverridden(), m.getPoints());
    }
}
