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
 * 치지직 OAuth 토큰 교환 + 프로필/채널 조회. (공식문서 chzzk.gitbook.io 대조 완료 2026-07-05)
 * - users/me(Bearer): channelId·channelName 만 반환 — 프로필 이미지는 없음.
 * - channels(Client-Id/Client-Secret 헤더): followerCount + channelImageUrl → 등급 산정·프사 자동 수집.
 * - 채널 응답은 인메모리 캐시(followerCacheSeconds)로 429 quota 방어.
 * - 조회 실패/채널 없음 → null 필드로 반환(로그인은 성공 처리, VIEWER 폴백).
 */
@Slf4j
@Component
public class ChzzkOAuthClient {

    private final ChzzkProperties props;
    private final RestClient tokenClient;
    private final RestClient apiClient;

    // channelId -> (팔로워/채널이미지, expiryMillis)
    private final Map<String, ChannelCacheEntry> channelCache = new ConcurrentHashMap<>();

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

        // 프사·팔로워는 채널 API에서 (users/me 엔 이미지 필드 없음)
        ChannelInfo channel = (channelId != null) ? fetchChannelInfo(channelId) : ChannelInfo.EMPTY;
        return new ChzzkProfile(channelId, nickname, channel.imageUrl(), channel.followerCount());
    }

    /** 채널 이미지 URL 조회(프사 복원용). 실패 → null. */
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
                    .uri(uriBuilder -> uriBuilder.path("/open/v1/channels")
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
            log.warn("chzzk channel info failed (fallback VIEWER): {}", e.getMessage());
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
