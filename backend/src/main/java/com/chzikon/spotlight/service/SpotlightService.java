package com.chzikon.spotlight.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.util.ExternalUrlValidator;
import com.chzikon.member.domain.Member;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.member.service.MemberService;
import com.chzikon.spotlight.domain.Spotlight;
import com.chzikon.spotlight.dto.SpotlightDtos.SpotlightCreateRequest;
import com.chzikon.spotlight.dto.SpotlightDtos.SpotlightResponse;
import com.chzikon.spotlight.repository.SpotlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotlightService {

    /** 사이드바 노출 최대 개수. */
    private static final int ACTIVE_LIMIT = 2;

    private final SpotlightRepository spotlightRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final ExternalUrlValidator urlValidator;
    private final AdminLogService adminLogService;
    private final com.chzikon.admin.service.AppSettingService appSettingService;

    /** 노출 중 스포트라이트(최대 2개) — 공개. */
    @Transactional(readOnly = true)
    public List<SpotlightResponse> findActive() {
        List<Spotlight> active = spotlightRepository.findActive(
                LocalDateTime.now(), PageRequest.of(0, ACTIVE_LIMIT));
        return withMembers(active);
    }

    /**
     * 등록 — STREAMER+ 백엔드 재검증(프론트 가드 불신뢰).
     * 1인 1활성: 노출 중인 내 스포트라이트가 있으면 충돌.
     */
    @Transactional
    public SpotlightResponse create(SpotlightCreateRequest request, Long memberId) {
        Member member = memberService.getById(memberId);
        if (!member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        urlValidator.validate(request.streamUrl());
        if (spotlightRepository.existsByMemberIdAndExpiresAtAfter(memberId, LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.SPOTLIGHT_ACTIVE_EXISTS);
        }
        // 포인트 차감(V15) — 어드민은 면제, 부족하면 POINT_INSUFFICIENT
        if (member.getRole() != com.chzikon.member.domain.Role.ADMIN) {
            member.spendPoints(appSettingService.getInt("SPOTLIGHT_POINT_COST", 50));
        }
        Spotlight saved = spotlightRepository.save(
                new Spotlight(memberId, request.title(), request.platform(), request.streamUrl(),
                        request.scheduledAt()));
        return SpotlightResponse.of(saved, member);
    }

    /** 어드민 목록 — 최근 50건. */
    @Transactional(readOnly = true)
    public List<SpotlightResponse> findRecentForAdmin() {
        return withMembers(spotlightRepository.findTop50ByOrderByCreatedAtDesc());
    }

    /** 어드민 승인 — 승인 시각부터 2시간 노출(승인제, 항목 12). */
    @Transactional
    public void approve(Long id, Long actorId) {
        Spotlight spotlight = spotlightRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        spotlight.approve();
        adminLogService.record(actorId, "SPOTLIGHT_APPROVE", "spotlight", id, spotlight.getTitle());
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Spotlight spotlight = spotlightRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        spotlightRepository.delete(spotlight);
        adminLogService.record(actorId, "SPOTLIGHT_DELETE", "spotlight", id, spotlight.getTitle());
    }

    private List<SpotlightResponse> withMembers(List<Spotlight> spotlights) {
        Map<Long, Member> members = memberRepository.findAllById(
                        spotlights.stream().map(Spotlight::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        return spotlights.stream()
                .map(s -> SpotlightResponse.of(s, members.get(s.getMemberId())))
                .toList();
    }
}
