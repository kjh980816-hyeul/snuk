package com.chzikon.auth.client;

import com.chzikon.member.domain.Provider;

/**
 * 플랫폼(치지직/씨미/숲)에서 가져온 로그인 사용자 프로필 (정규화 결과).
 * followerCount 는 nullable (채널 없음/조회 실패 → 호출측에서 VIEWER 폴백).
 */
public record OAuthProfile(
        Provider provider,
        String channelId,
        String nickname,
        String profileImageUrl,
        Integer followerCount
) {
}
