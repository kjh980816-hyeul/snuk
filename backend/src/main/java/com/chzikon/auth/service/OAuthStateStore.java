package com.chzikon.auth.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OAuth state(CSRF 방어) 발급/검증. SecureRandom 생성 + 서버 보관 + TTL.
 * (단일 인스턴스 가정. 다중 인스턴스 확장 시 Redis 로 교체 — chzzk-integration.md)
 */
@Component
public class OAuthStateStore {

    private static final long TTL_MILLIS = 5 * 60 * 1000L; // 5분
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Long> issued = new ConcurrentHashMap<>();

    public String issue() {
        purgeExpired();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        issued.put(state, System.currentTimeMillis() + TTL_MILLIS);
        return state;
    }

    /** 1회성 검증: 존재하고 만료 전이면 true 후 즉시 제거. */
    public boolean consume(String state) {
        if (state == null) {
            return false;
        }
        Long expiry = issued.remove(state);
        return expiry != null && expiry >= System.currentTimeMillis();
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        issued.entrySet().removeIf(e -> e.getValue() < now);
    }
}
