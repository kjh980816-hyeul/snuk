package com.chzikon.auth.client;

import com.chzikon.auth.config.ChzzkProperties;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 치지직 OAuth 토큰 교환 + 프로필/팔로워 조회.
 *
 * ⚠️ 엔드포인트/필드명은 chzzk-integration.md 기준 best-guess.
 *    구현 직전 공식 문서(developers.chzzk.naver.com / chzzk.gitbook.io)로 반드시 재확인.
 * - Authorization: "Bearer <token>" (Bearer 와 토큰 사이 공백 필수)
 * - 팔로워 응답은 인메모리 캐시(followerCacheSeconds)로 429 quota 방어.
 * - 조회 실패/채널 없음 → followerCount=null 로 반환(로그인은 성공 처리).
 */
@Slf4j
@Component
public class ChzzkOAuthClient {

    private final ChzzkProperties props;
    private final RestClient tokenClient;
    private final RestClient apiClient;

    // channelId -> (followerCount, expiryMillis)
    private final Map<String, FollowerCacheEntry> followerCache = new ConcurrentHashMap<>();

    public ChzzkOAuthClient(ChzzkProperties props, RestClient.Builder builder) {
        this.props = props;
        this.tokenClient = builder.clone().baseUrl(props.tokenUri()).build();
        this.apiClient = builder.clone().baseUrl(props.apiBaseUri()).build();
    }

    /** 인가 시작 URL. */
    public String buildAuthorizationUrl(String state) {
        return props.authorizationUri()
                + "?clientId=" + enc(props.clientId())
                + "&redirectUri=" + enc(props.redirectUri())
                + "&state=" + enc(state);
    }

    /** Authorization Code → Access Token 교환 후, 프로필+팔로워 정규화 반환. */
    public ChzzkProfile exchangeAndFetchProfile(String code, String state) {
        String accessToken = exchangeToken(code, state);
        return fetchProfile(accessToken);
    }

    private String exchangeToken(String code, String state) {
        try {
            JsonNode res = tokenClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "grantType", "authorization_code",
                            "clientId", props.clientId(),
                            "clientSecret", props.clientSecret(),
                            "code", code,
                            "state", state))
                    .retrieve()
                    .body(JsonNode.class);
            String token = textAtAny(res, "accessToken", "access_token");
            // content.accessToken 형태 대비
            if (token == null && res != null && res.has("content")) {
                token = textAtAny(res.get("content"), "accessToken", "access_token");
            }
            if (token == null || token.isBlank()) {
                throw new BusinessException(ErrorCode.OAUTH_FAILED, "토큰 응답에 accessToken 이 없습니다.");
            }
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("chzzk token exchange failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    private ChzzkProfile fetchProfile(String accessToken) {
        JsonNode me;
        try {
            me = apiClient.get()
                    .uri("/open/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("chzzk user info failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
        JsonNode content = (me != null && me.has("content")) ? me.get("content") : me;
        String channelId = textAtAny(content, "channelId", "userIdHash", "id");
        String nickname = firstNonBlank(textAtAny(content, "nickname", "channelName"), "스트리머");
        String profileImage = textAtAny(content, "profileImageUrl", "channelImageUrl");

        Integer followerCount = (channelId != null) ? fetchFollowerCount(accessToken, channelId) : null;
        return new ChzzkProfile(channelId, nickname, profileImage, followerCount);
    }

    /** 팔로워 수 조회 (캐시). 실패/없음 → null (호출측 VIEWER 폴백). */
    private Integer fetchFollowerCount(String accessToken, String channelId) {
        FollowerCacheEntry cached = followerCache.get(channelId);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiryMillis >= now) {
            return cached.followerCount;
        }
        Integer value = null;
        try {
            JsonNode res = apiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/open/v1/channels")
                            .queryParam("channelIds", channelId).build())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = (res != null && res.has("content")) ? res.get("content") : res;
            JsonNode arr = (data != null && data.has("data")) ? data.get("data") : data;
            JsonNode first = (arr != null && arr.isArray() && !arr.isEmpty()) ? arr.get(0) : data;
            if (first != null && first.has("followerCount") && !first.get("followerCount").isNull()) {
                value = first.get("followerCount").asInt();
            }
        } catch (Exception e) {
            log.warn("chzzk follower count failed (fallback VIEWER): {}", e.getMessage());
        }
        followerCache.put(channelId,
                new FollowerCacheEntry(value, now + props.followerCacheSeconds() * 1000));
        return value;
    }

    private static String textAtAny(JsonNode node, String... fields) {
        if (node == null) {
            return null;
        }
        for (String f : fields) {
            JsonNode v = node.get(f);
            if (v != null && !v.isNull() && !v.asText().isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private static String enc(String v) {
        return java.net.URLEncoder.encode(v, java.nio.charset.StandardCharsets.UTF_8);
    }

    private record FollowerCacheEntry(Integer followerCount, long expiryMillis) {
    }
}
