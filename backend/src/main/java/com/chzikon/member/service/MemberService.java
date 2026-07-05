package com.chzikon.member.service;

import com.chzikon.auth.client.ChzzkProfile;
import com.chzikon.auth.service.RoleCalculator;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RoleCalculator roleCalculator;

    @Value("${app.admin.channel-id:}")
    private String adminChannelId;

    /** 로그인 콜백: 채널ID 기준 upsert. 권한 자동 재산정(오버라이드/ADMIN 제외). */
    @Transactional
    public Member upsertOnLogin(ChzzkProfile profile) {
        if (profile.channelId() == null || profile.channelId().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "치지직 채널 식별자를 확인할 수 없습니다.");
        }
        // 대표 채널이면 ADMIN 고정(ADM-01). 그 외에는 팔로워 기반 자동 산정.
        boolean isDesignatedAdmin = adminChannelId != null && !adminChannelId.isBlank()
                && adminChannelId.equals(profile.channelId());
        Role recomputed = isDesignatedAdmin ? Role.ADMIN : roleCalculator.compute(profile.followerCount());
        return memberRepository.findByChzzkChannelId(profile.channelId())
                .map(existing -> {
                    existing.refreshOnLogin(profile.nickname(), profile.profileImageUrl(),
                            profile.followerCount(), recomputed);
                    return existing;
                })
                .orElseGet(() -> memberRepository.save(Member.create(
                        profile.channelId(), profile.nickname(), profile.profileImageUrl(),
                        profile.followerCount(), recomputed)));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Member> list(org.springframework.data.domain.Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Member getById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public Member overrideRole(Long memberId, Role role) {
        Member member = getById(memberId);
        member.overrideRole(role);
        return member;
    }

    @Transactional
    public Member clearOverride(Long memberId) {
        Member member = getById(memberId);
        member.clearOverride();
        return member;
    }
}
