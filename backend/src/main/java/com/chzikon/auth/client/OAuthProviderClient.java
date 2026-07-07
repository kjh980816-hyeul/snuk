package com.chzikon.auth.client;

import com.chzikon.member.domain.Provider;

/**
 * 플랫폼별 OAuth 클라이언트 공통 계약 (치지직/씨미/숲).
 * 플로우: buildAuthorizationUrl → 콜백 code 교환 → OAuthProfile 정규화 → member upsert.
 */
public interface OAuthProviderClient {

    Provider provider();

    /** 인가 시작 URL. state 미지원 플랫폼(숲)은 state 를 무시한다. */
    String buildAuthorizationUrl(String state);

    /** Authorization Code → Access Token 교환 후 프로필+팔로워 정규화. */
    OAuthProfile exchangeAndFetchProfile(String code, String state);

    /** 콜백 state(CSRF) 검증 여부. 숲은 인가 요청에 state 파라미터가 없어 false. */
    default boolean requiresState() {
        return true;
    }

    /** 프사 복원용 채널 이미지 조회(유저 토큰 없이). 미지원 플랫폼(숲)은 null. */
    default String fetchChannelImageUrl(String channelId) {
        return null;
    }
}
