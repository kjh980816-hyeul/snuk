package com.chzikon.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 씨미(CIME, ci.me) OAuth/API 설정. 시크릿은 환경변수로 주입(application.yml 의 ${...}).
 * 공식문서 developers.ci.me 대조(2026-07-07). 실키 발급 후 1회 실측 검증 필요.
 */
@ConfigurationProperties(prefix = "app.cime")
public record CimeProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationUri,
        String tokenUri,
        String apiBaseUri,
        long followerCacheSeconds
) {
}
