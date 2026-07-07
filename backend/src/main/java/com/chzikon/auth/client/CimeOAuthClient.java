package com.chzikon.auth.client;

import com.chzikon.auth.config.CimeProperties;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Provider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 씨미(CIME, ci.me) OAuth 토큰 교환 + 프로필/채널 조회. (공식문서 developers.ci.me 대조 2026-07-07)
 * 치지직과 사실상 동일 구조:
 * - authorize: /auth/openapi/account-interlock?clientId&redirectUri&state
 * - token: POST /api/openapi/auth/v1/token (JSON camelCase: grantType/clientId/clientSecret/code)
 * - users/me(Bearer): channelId·channelName·channelImageUrl (치지직과 달리 프사가 여기 바로 있음)
 * - channels(Client-Id/Client-Secret 헤더): followerCount → 등급 산정. 인메모리 캐시로 quota 방어.
 * ⚠️ 실키 발급 전 미실측 — 채널 조회 헤더명(Client-Id/Client-Secret)은 치지직 관례 준용, 실연동 시 1회 검증.
 */
@Slf4j
@Component
public class CimeOAuthClient implements OAuthProviderClient {

    private final CimeProperties props;
    private final RestClient tokenClient;
    private final RestClient apiClient;

    // channelId -> (팔로워/채널이미지, expiryMillis)
    private final Map<String, ChannelCacheEntry> channelCache = new ConcurrentHashMap<>();

    public CimeOAuthClient(CimeProperties props, RestClient.Builder builder) {
        this.props = props;
        this.tokenClient = builder.clone().baseUrl(props.tokenUri()).build();
        this.apiClient = builder.clone().baseUrl(props.apiBaseUri()).build();
    }

    @Override
    public Provider provider() {
        return Provider.CIME;
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return props.authorizationUri()
                + "?clientId=" + enc(props.clientId())
                + "&redirectUri=" + enc(props.redirectUri())
                + "&state=" + enc(state);
    }

    @Override
    public OAuthProfile exchangeAndFetchProfile(String code, String state) {
        String accessToken = exchangeToken(code, state);
        return fetchProfile(accessToken);
    }

    private String exchangeToken(String code, String state) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("grantType", "authorization_code");
            body.put("clientId", props.clientId());
            body.put("clientSecret", props.clientSecret());
            body.put("code", code);
            if (state != null && !state.isBlank()) {
                body.put("state", state);
            }
            JsonNode res = tokenClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);
            String token = textAtAny(res, "accessToken", "access_token");
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
            log.warn("cime token exchange failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    private OAuthProfile fetchProfile(String accessToken) {
        JsonNode me;
        try {
            me = apiClient.get()
                    .uri("/api/openapi/open/v1/users/me")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("cime user info failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
        JsonNode content = (me != null && me.has("content")) ? me.get("content") : me;
        JsonNode data = (content != null && content.has("data")) ? content.get("data") : content;
        String channelId = textAtAny(data, "channelId", "id");
        String nickname = firstNonBlank(textAtAny(data, "channelName", "nickname"), "스트리머");
        String meImage = textAtAny(data, "channelImageUrl", "profileImageUrl");

        ChannelInfo channel = (channelId != null) ? fetchChannelInfo(channelId) : ChannelInfo.EMPTY;
        String imageUrl = firstNonBlank(meImage, channel.imageUrl());
        return new OAuthProfile(Provider.CIME, channelId, nickname, imageUrl, channel.followerCount());
    }

    /** 채널 이미지 URL 조회(프사 복원용). 실패 → null. */
    @Override
    public String fetchChannelImageUrl(String channelId) {
        return fetchChannelInfo(channelId).imageUrl();
    }

    /** 채널 정보(팔로워 수 + 채널 이미지) 조회 — Client 자격 인증, 캐시. 실패 → null 필드(VIEWER 폴백). */
    private ChannelInfo fetchChannelInfo(String channelId) {
        ChannelCacheEntry cached = channelCache.get(channelId);
        long now = System.currentTimeMillis();
        if (cached != null && cached.expiryMillis >= now) {
            return cached.info;
        }
        ChannelInfo info = ChannelInfo.EMPTY;
        try {
            JsonNode res = apiClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/api/openapi/open/v1/channels")
                            .queryParam("channelIds", channelId).build())
                    .header("Client-Id", props.clientId())
                    .header("Client-Secret", props.clientSecret())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode data = (res != null && res.has("content")) ? res.get("content") : res;
            JsonNode arr = (data != null && data.has("data")) ? data.get("data") : data;
            JsonNode first = (arr != null && arr.isArray() && !arr.isEmpty()) ? arr.get(0) : data;
            if (first != null) {
                Integer followers = (first.has("followerCount") && !first.get("followerCount").isNull())
                        ? first.get("followerCount").asInt() : null;
                info = new ChannelInfo(followers, textAtAny(first, "channelImageUrl", "profileImageUrl"));
            }
        } catch (Exception e) {
            log.warn("cime channel info failed (fallback VIEWER): {}", e.getMessage());
        }
        channelCache.put(channelId,
                new ChannelCacheEntry(info, now + props.followerCacheSeconds() * 1000));
        return info;
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

    private record ChannelInfo(Integer followerCount, String imageUrl) {
        static final ChannelInfo EMPTY = new ChannelInfo(null, null);
    }

    private record ChannelCacheEntry(ChannelInfo info, long expiryMillis) {
    }
}
