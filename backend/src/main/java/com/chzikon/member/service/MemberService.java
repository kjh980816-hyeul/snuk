package com.chzikon.member.service;

import com.chzikon.auth.client.OAuthProfile;
import com.chzikon.auth.client.OAuthProviderClient;
import com.chzikon.auth.service.RoleCalculator;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final RoleCalculator roleCalculator;
    private final List<OAuthProviderClient> oauthClients;
    private final com.chzikon.global.util.ExternalUrlValidator urlValidator;
    private final com.chzikon.global.upload.FileStorageService fileStorage;
    private final com.chzikon.admin.service.AppSettingService appSettingService;

    @Value("${app.admin.channel-id:}")
    private String adminChannelId;

    /** 로그인 콜백: (provider, 채널ID) 기준 upsert. 권한 자동 재산정(오버라이드/ADMIN 제외). */
    @Transactional
    public Member upsertOnLogin(OAuthProfile profile) {
        if (profile.channelId() == null || profile.channelId().isBlank()) {
            throw new BusinessException(ErrorCode.OAUTH_FAILED, "플랫폼 채널 식별자를 확인할 수 없습니다.");
        }
        // 대표 치지직 채널이면 ADMIN 고정(ADM-01). 그 외에는 팔로워 기반 자동 산정.
        boolean isDesignatedAdmin = profile.provider() == Provider.CHZZK
                && adminChannelId != null && !adminChannelId.isBlank()
                && adminChannelId.equals(profile.channelId());
        Role recomputed = isDesignatedAdmin ? Role.ADMIN : roleCalculator.compute(profile.followerCount());
        Member member = memberRepository.findByProviderAndChannelId(profile.provider(), profile.channelId())
                .map(existing -> {
                    existing.refreshOnLogin(profile.nickname(), profile.profileImageUrl(),
                            profile.followerCount(), recomputed);
                    return existing;
                })
                .orElseGet(() -> memberRepository.save(Member.create(
                        profile.provider(), profile.channelId(), profile.nickname(),
                        profile.profileImageUrl(), profile.followerCount(), recomputed)));
        // 하루 첫 로그인 포인트 적립(V15) — 금액은 어드민 설정 POINT_DAILY_AMOUNT
        member.grantDailyPoint(appSettingService.getInt("POINT_DAILY_AMOUNT", 10), java.time.LocalDate.now());
        return member;
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

    /** 프사 직접 변경(URL 검증). 이후 로그인 동기화가 덮어쓰지 않음. */
    @Transactional
    public Member changeProfileImage(Long memberId, String imageUrl) {
        urlValidator.validate(imageUrl);
        Member member = getById(memberId);
        member.changeProfileImage(imageUrl);
        return member;
    }

    /** 프사 파일 업로드 적용. 이전 업로드 파일은 정리. */
    @Transactional
    public Member changeProfileImageUpload(Long memberId, org.springframework.web.multipart.MultipartFile file) {
        Member member = getById(memberId);
        String old = member.getProfileImageUrl();
        member.changeProfileImage(fileStorage.storeImage(file));
        fileStorage.deleteIfLocal(old);
        return member;
    }

    /** 플랫폼 프사로 복원 + 동기화 재개. 이전 업로드 파일은 정리. (숲은 재조회 미지원 → 다음 로그인 때 갱신) */
    @Transactional
    public Member resetProfileImage(Long memberId) {
        Member member = getById(memberId);
        String old = member.getProfileImageUrl();
        String restored = oauthClients.stream()
                .filter(c -> c.provider() == member.getProvider())
                .findFirst()
                .map(c -> c.fetchChannelImageUrl(member.getChannelId()))
                .orElse(null);
        member.resetProfileImage(restored);
        fileStorage.deleteIfLocal(old);
        return member;
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
