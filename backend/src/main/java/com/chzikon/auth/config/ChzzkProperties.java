package com.chzikon.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 치지직 OAuth/API 설정. 시크릿은 환경변수로 주입(application.yml 의 ${...}).
 * 실제 엔드포인트/필드는 구현 직전 공식 문서(chzzk.gitbook.io)로 재확인 — chzzk-integration.md.
 */
@ConfigurationProperties(prefix = "app.chzzk")
public record ChzzkProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationUri,
        String tokenUri,
        String apiBaseUri,
        long followerCacheSeconds
) {
}
