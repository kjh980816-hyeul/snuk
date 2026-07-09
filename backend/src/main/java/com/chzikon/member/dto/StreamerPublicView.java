package com.chzikon.member.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;

/** 공개 스트리머 카드(스트리머 섹션) — 민감정보 없이 노출용 필드만. */
public record StreamerPublicView(
        Long id,
        String nickname,
        String profileImageUrl,
        String provider,
        Integer followerCount,
        String channelUrl
) {
    public static StreamerPublicView from(Member m) {
        return new StreamerPublicView(m.getId(), m.getNickname(), m.getProfileImageUrl(),
                m.getProvider().name(), m.getFollowerCount(), channelUrlOf(m));
    }

    private static String channelUrlOf(Member m) {
        if (m.getProvider() == Provider.CHZZK) {
            return "https://chzzk.naver.com/" + m.getChannelId();
        }
        if (m.getProvider() == Provider.SOOP) {
            return "https://ch.sooplive.co.kr/" + m.getChannelId();
        }
        return null; // CIME — 공개 채널 URL 패턴 미확정
    }
}
