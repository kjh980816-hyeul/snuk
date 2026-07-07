package com.chzikon.auth.client;

import com.chzikon.auth.config.SoopProperties;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Provider;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 숲(SOOP) Open API OAuth 토큰 교환 + 방송국 정보 조회. (공식문서 openapi.sooplive.co.kr/apidoc 대조 2026-07-07)
 * 치지직/씨미와 다른 점:
 * - authorize: GET {authorizationUri}?client_id=... 만 — state 파라미터 미지원(requiresState=false),
 *   redirect_uri 는 개발자센터 '내 계정 관리' 등록값 사용.
 * - token: POST /auth/token, application/x-www-form-urlencoded (snake_case).
 * - 프로필: POST /user/stationinfo → data.user_nick/profile_image/favorite_cnt(즐겨찾기=팔로워 상당).
 * ⚠️ 문서상 stationinfo 응답에 user_id 필드가 명시돼 있지 않음 — 후보 필드(user_id/userId/id/bj_id)를
 *   방어적으로 파싱하고, 전부 없으면 명확한 에러로 실패시킴. 제휴 승인 후 실측으로 확정할 것.
 * - 채널 이미지 재조회(fetchChannelImageUrl)는 유저 토큰 없인 불가 → 기본 null(다음 로그인 때 갱신).
 */
@Slf4j
@Component
public class SoopOAuthClient implements OAuthProviderClient {

    private final SoopProperties props;
    private final RestClient apiClient;

    public SoopOAuthClient(SoopProperties props, RestClient.Builder builder) {
        this.props = props;
        this.apiClient = builder.clone().baseUrl(props.apiBaseUri()).build();
    }

    @Override
    public Provider provider() {
        return Provider.SOOP;
    }

    @Override
    public boolean requiresState() {
        return false; // 숲 인가 요청은 state 미지원 (code 는 1회용)
    }

    @Override
    public String buildAuthorizationUrl(String state) {
        return props.authorizationUri() + "?client_id=" + enc(props.clientId());
    }

    @Override
    public OAuthProfile exchangeAndFetchProfile(String code, String state) {
        String accessToken = exchangeToken(code);
        return fetchProfile(accessToken);
    }

    private String exchangeToken(String code) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("grant_type", "authorization_code");
            form.add("client_id", props.clientId());
            form.add("client_secret", props.clientSecret());
            form.add("code", code);
            if (props.redirectUri() != null && !props.redirectUri().isBlank()) {
                form.add("redirect_uri", props.redirectUri());
            }
            JsonNode res = apiClient.post()
                    .uri("/auth/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            String token = textAtAny(res, "access_token", "accessToken");
            if (token == null && res != null && res.has("data")) {
                token = textAtAny(res.get("data"), "access_token", "accessToken");
            }
            if (token == null || token.isBlank()) {
                throw new BusinessException(ErrorCode.OAUTH_FAILED, "토큰 응답에 access_token 이 없습니다.");
            }
            return token;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("soop token exchange failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
    }

    private OAuthProfile fetchProfile(String accessToken) {
        JsonNode res;
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("access_token", accessToken); // 문서: 바디 파라미터. Bearer 헤더도 함께(방어적).
            res = apiClient.post()
                    .uri("/user/stationinfo")
                    .header("Authorization", "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (Exception e) {
            log.warn("soop station info failed: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OAUTH_FAILED);
        }
        JsonNode data = (res != null && res.has("data")) ? res.get("data") : res;
        String channelId = firstNonBlank(
                textAtAny(data, "user_id", "userId", "id", "bj_id"),
                textAtAny(res, "user_id", "userId", "id", "bj_id"));
        if (channelId == null) {
            // 문서에 식별자 필드 미명시 — 실측 시 응답 키를 로그로 확인(값은 남기지 않음)
            log.warn("soop stationinfo has no user id field. keys={}",
                    data != null ? String.join(",", iterableToList(data)) : "null");
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "숲 응답에서 사용자 식별자를 찾을 수 없습니다.");
        }
        String nickname = firstNonBlank(textAtAny(data, "user_nick", "nickname", "station_name"), "스트리머");
        String imageUrl = normalizeUrl(textAtAny(data, "profile_image", "profileImage"));
        Integer favoriteCnt = (data != null && data.has("favorite_cnt") && !data.get("favorite_cnt").isNull())
                ? data.get("favorite_cnt").asInt() : null;
        return new OAuthProfile(Provider.SOOP, channelId, nickname, imageUrl, favoriteCnt);
    }

    /** 프로토콜 생략(//host/...) 이미지 URL 보정. */
    private static String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        return url.startsWith("//") ? "https:" + url : url;
    }

    private static java.util.List<String> iterableToList(JsonNode node) {
        java.util.List<String> keys = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(keys::add);
        return keys;
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
}
