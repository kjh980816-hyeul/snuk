package com.chzikon.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 숲(SOOP) Open API 설정. 시크릿은 환경변수로 주입(application.yml 의 ${...}).
 * 공식문서 openapi.sooplive.co.kr/apidoc 대조(2026-07-07). 제휴 승인 후 실측 검증 필요.
 * redirect-uri 는 개발자센터 '내 계정 관리'에 등록하는 값(토큰 교환 시 optional 파라미터).
 */
@ConfigurationProperties(prefix = "app.soop")
public record SoopProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String authorizationUri,
        String apiBaseUri
) {
}
