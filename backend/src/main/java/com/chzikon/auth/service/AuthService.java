package com.chzikon.auth.service;

import com.chzikon.auth.client.ChzzkOAuthClient;
import com.chzikon.auth.client.ChzzkProfile;
import com.chzikon.auth.dto.TokenPair;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.security.JwtTokenProvider;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ChzzkOAuthClient chzzkClient;
    private final OAuthStateStore stateStore;
    private final MemberService memberService;
    private final JwtTokenProvider tokenProvider;

    /** 인가 시작 URL (state 발급 포함). */
    public String buildAuthorizationUrl() {
        String state = stateStore.issue();
        return chzzkClient.buildAuthorizationUrl(state);
    }

    /** 콜백 처리: state 검증 → 프로필 조회 → 회원 upsert → 토큰 발급. */
    public TokenPair handleCallback(String code, String state) {
        if (!stateStore.consume(state)) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "state 검증 실패(CSRF 의심).");
        }
        ChzzkProfile profile = chzzkClient.exchangeAndFetchProfile(code, state);
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

    private TokenPair issue(Member member) {
        Role role = member.getRole();
        return new TokenPair(
                tokenProvider.createAccessToken(member.getId(), role),
                tokenProvider.createRefreshToken(member.getId(), role));
    }
}
