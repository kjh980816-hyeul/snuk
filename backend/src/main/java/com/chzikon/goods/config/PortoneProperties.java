package com.chzikon.goods.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * PortOne(V2) 결제 설정.
 * - storeId / channelKey : 공개값(프론트 SDK 결제창 호출에 필요) → 주문 응답에 포함해 내려줌.
 * - apiSecret            : 비밀값(서버 결제 검증 API 인증). 환경변수로만 주입, 응답/로그 노출 금지.
 * 실제 값은 PortOne 콘솔(admin.portone.io)에서 발급.
 */
@ConfigurationProperties(prefix = "app.portone")
public record PortoneProperties(
        String storeId,
        String channelKey,
        String apiSecret,
        String apiBaseUri
) {
}
