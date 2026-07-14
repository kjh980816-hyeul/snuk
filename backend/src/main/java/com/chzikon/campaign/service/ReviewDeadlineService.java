package com.chzikon.campaign.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.Campaign;
import com.chzikon.campaign.domain.CampaignApplication;
import com.chzikon.campaign.dto.ReviewWarningAdminView;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.review.domain.PostCategory;
import com.chzikon.review.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 게임체험단 후기 마감 관리.
 * - 키 배정 시 마감 = 배정 시각 + 30일 (CampaignApplication.approve)
 * - 마감 경과 + 후기 미작성 → 경고 기록(스윕 1시간 주기, 어드민 경고 로그 노출)
 * - 마이페이지 연장: 게임(캠페인)당 1회, 7일
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewDeadlineService {

    private final CampaignApplicationRepository applicationRepository;
    private final CampaignRepository campaignRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final AdminLogService adminLogService;

    /** 마감 경과·미경고 건 스윕 — 후기 미작성이면 경고 기록. */
    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    @Transactional
    public void sweepOverdue() {
        List<CampaignApplication> overdue = applicationRepository
                .findByStatusAndReviewDeadlineBeforeAndWarnedAtIsNull(
                        CampaignApplication.Status.APPROVED, LocalDateTime.now());
        int warned = 0;
        for (CampaignApplication app : overdue) {
            boolean reviewWritten = postRepository.existsByCategoryAndCampaignIdAndMemberId(
                    PostCategory.REVIEW, app.getCampaignId(), app.getMemberId());
            if (reviewWritten) {
                continue;
            }
            app.markWarned();
            adminLogService.record(null, "REVIEW_OVERDUE_WARNING", "campaign_application", app.getId(),
                    "member=" + app.getMemberId() + " campaign=" + app.getCampaignId()
                            + " deadline=" + app.getReviewDeadline());
            warned++;
        }
        if (warned > 0) {
            log.info("[review-deadline] 후기 미작성 경고 {}건 기록", warned);
        }
    }

    /** 마이페이지 연장 — 본인 신청 + 후기 미작성 + 미연장 건만. 새 마감 반환. */
    @Transactional
    public LocalDateTime extend(Long applicationId, Long memberId) {
        CampaignApplication app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!app.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (app.getReviewDeadline() == null) {
            throw new BusinessException(ErrorCode.DEADLINE_NOT_APPLICABLE);
        }
        boolean reviewWritten = postRepository.existsByCategoryAndCampaignIdAndMemberId(
                PostCategory.REVIEW, app.getCampaignId(), app.getMemberId());
        if (reviewWritten) {
            throw new BusinessException(ErrorCode.DEADLINE_NOT_APPLICABLE);
        }
        app.extendDeadline(); // 1회 제한은 도메인에서 검증
        return app.getReviewDeadline();
    }

    /** 어드민 경고 로그 — 경고 발생 건 전체(후기 뒤늦게 작성 여부 포함). */
    @Transactional(readOnly = true)
    public List<ReviewWarningAdminView> warnings() {
        List<CampaignApplication> apps = applicationRepository.findByWarnedAtIsNotNullOrderByWarnedAtDesc();
        Map<Long, Campaign> campaigns = campaignRepository.findAllById(
                        apps.stream().map(CampaignApplication::getCampaignId).distinct().toList())
                .stream().collect(Collectors.toMap(Campaign::getId, Function.identity()));
        Map<Long, Member> members = memberRepository.findAllById(
                        apps.stream().map(CampaignApplication::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        return apps.stream().map(a -> {
            Campaign c = campaigns.get(a.getCampaignId());
            Member m = members.get(a.getMemberId());
            boolean reviewWritten = postRepository.existsByCategoryAndCampaignIdAndMemberId(
                    PostCategory.REVIEW, a.getCampaignId(), a.getMemberId());
            return new ReviewWarningAdminView(a.getId(), a.getMemberId(),
                    m != null ? m.getNickname() : "(탈퇴 회원)",
                    a.getCampaignId(), c != null ? c.getTitle() : "(삭제된 캠페인)",
                    a.getReviewDeadline(), a.getWarnedAt(), a.isDeadlineExtended(), reviewWritten);
        }).toList();
    }
}
