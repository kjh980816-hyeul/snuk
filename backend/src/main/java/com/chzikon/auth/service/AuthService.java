package com.chzikon.auth.service;

import com.chzikon.auth.client.OAuthProfile;
import com.chzikon.auth.client.OAuthProviderClient;
import com.chzikon.auth.dto.TokenPair;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.security.JwtTokenProvider;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final Map<Provider, OAuthProviderClient> clients;
    private final OAuthStateStore stateStore;
    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;

    public AuthService(List<OAuthProviderClient> clientList, OAuthStateStore stateStore,
                       MemberService memberService, JwtTokenProvider tokenProvider) {
        this.clients = clientList.stream()
                .collect(Collectors.toUnmodifiableMap(OAuthProviderClient::provider, Function.identity()));
        this.stateStore = stateStore;
        this.memberService = memberService;
        this.tokenProvider = tokenProvider;
    }

    /** 인가 시작 URL (state 발급 포함 — state 미지원 플랫폼은 클라이언트가 무시). */
    public String buildAuthorizationUrl(Provider provider) {
        return clientOf(provider).buildAuthorizationUrl(stateStore.issue());
    }

    /** 콜백 처리: state 검증(지원 플랫폼만) → 프로필 조회 → 회원 upsert → 토큰 발급. */
    public TokenPair handleCallback(Provider provider, String code, String state) {
        OAuthProviderClient client = clientOf(provider);
        if (client.requiresState() && !stateStore.consume(state)) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "state 검증 실패(CSRF 의심).");
        }
        OAuthProfile profile = client.exchangeAndFetchProfile(code, state);
        Member member = memberService.upsertOnLogin(profile);
        return issue(member);
    }

    /** Refresh 토큰으로 Access/Refresh 재발급. */
    public TokenPair refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        try {
            Claims claims = tokenProvider.parse(refreshToken);
            if (!tokenProvider.isRefreshToken(claims)) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }
            Long memberId = Long.valueOf(claims.getSubject());
            Member member = memberService.getById(memberId);  // 최신 role 반영
            return issue(member);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }

    private OAuthProviderClient clientOf(Provider provider) {
        OAuthProviderClient client = (provider == null) ? null : clients.get(provider);
        if (client == null) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "지원하지 않는 로그인 플랫폼입니다.");
        }
        return client;
    }

    private TokenPair issue(Member member) {
        Role role = member.getRole();
        return new TokenPair(
                tokenProvider.createAccessToken(member.getId(), role),
                tokenProvider.createRefreshToken(member.getId(), role));
    }
}
