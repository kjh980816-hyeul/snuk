package com.chzikon.campaign.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.*;
import com.chzikon.campaign.dto.ApplicationAdminView;
import com.chzikon.campaign.dto.MyApplicationResponse;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.campaign.repository.GameKeyRepository;
import com.chzikon.global.crypto.KeyCipher;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CampaignApplicationService {

    private final CampaignRepository campaignRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final GameKeyRepository gameKeyRepository;
    private final MemberService memberService;
    private final KeyCipher keyCipher;
    private final AdminLogService adminLogService;

    /**
     * 캠페인 신청 (CMP-05/06/08).
     * - 권한 STREAMER+ 백엔드 재검증(role 은 팔로워 임계값+오버라이드를 이미 반영. 프론트 가드 불신뢰).
     * - 1인 1신청(unique). FCFS 는 비관적 락으로 슬롯/키 원자 배정, APPROVAL 은 PENDING.
     */
    @Transactional
    public CampaignApplication apply(Long campaignId, Long memberId) {
        Member member = memberService.getById(memberId);
        if (!member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        if (applicationRepository.existsByCampaignIdAndMemberId(campaignId, memberId)) {
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }

        // 슬롯/상태 경쟁 자원은 락을 잡고 재확인 (ADR-007)
        Campaign campaign = campaignRepository.findByIdForUpdate(campaignId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!campaign.isOpenForApply()) {
            throw new BusinessException(ErrorCode.CAMPAIGN_NOT_OPEN);
        }

        int followerSnapshot = member.getFollowerCount() != null ? member.getFollowerCount() : 0;

        CampaignApplication application;
        if (campaign.getDistributionType() == DistributionType.FCFS) {
            campaign.fillOneSlot(); // 슬롯 소진 시 CAMPAIGN_FULL
            Long assignedKeyId = null;
            if (campaign.getKeyMode() == KeyMode.UNIQUE_KEY) {
                assignedKeyId = assignAvailableKey(campaignId, memberId);
            }
            application = new CampaignApplication(campaignId, memberId, followerSnapshot,
                    CampaignApplication.Status.PENDING);
            application.approve(assignedKeyId);
        } else {
            // APPROVAL: 접수만. 슬롯/키는 대표 승인 시 배정.
            application = new CampaignApplication(campaignId, memberId, followerSnapshot,
                    CampaignApplication.Status.PENDING);
        }

        try {
            return applicationRepository.save(application);
        } catch (DataIntegrityViolationException e) {
            // unique(campaign_id, member_id) 동시 중복신청 방어
            throw new BusinessException(ErrorCode.ALREADY_APPLIED);
        }
    }

    /** 가용 키 1개를 락으로 선점 후 배정. 없으면 예외(슬롯은 상위 트랜잭션 롤백으로 환원). */
    private Long assignAvailableKey(Long campaignId, Long memberId) {
        List<GameKey> available = gameKeyRepository.findAvailableForUpdate(
                campaignId, GameKey.Status.AVAILABLE, PageRequest.of(0, 1));
        if (available.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_KEY);
        }
        GameKey key = available.get(0);
        key.assignTo(memberId);
        return key.getId();
    }

    // ---------- 승인제(APPROVAL) 대표 처리 ----------

    @Transactional
    public void approve(Long applicationId, Long actorId) {
        CampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (application.isApproved()) {
            return;
        }
        Campaign campaign = campaignRepository.findByIdForUpdate(application.getCampaignId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        campaign.fillOneSlot();
        Long assignedKeyId = null;
        if (campaign.getKeyMode() == KeyMode.UNIQUE_KEY) {
            assignedKeyId = assignAvailableKey(campaign.getId(), application.getMemberId());
        }
        application.approve(assignedKeyId);
        adminLogService.record(actorId, "APPLICATION_APPROVE", "campaign_application", applicationId,
                "campaign=" + campaign.getId() + " member=" + application.getMemberId());
    }

    @Transactional
    public void reject(Long applicationId, Long actorId) {
        CampaignApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        application.reject();
        adminLogService.record(actorId, "APPLICATION_REJECT", "campaign_application", applicationId,
                "member=" + application.getMemberId());
    }

    @Transactional(readOnly = true)
    public List<ApplicationAdminView> listApplications(Long campaignId) {
        return applicationRepository.findByCampaignIdOrderByAppliedAtAsc(campaignId).stream()
                .map(ApplicationAdminView::from)
                .toList();
    }

    // ---------- 본인 조회 ----------

    @Transactional(readOnly = true)
    public Optional<MyApplicationResponse> getMyApplication(Long campaignId, Long memberId, boolean reveal) {
        return applicationRepository.findByCampaignIdAndMemberId(campaignId, memberId)
                .map(app -> {
                    if (app.getAssignedKeyId() == null) {
                        return MyApplicationResponse.masked(app, false, null);
                    }
                    GameKey key = gameKeyRepository.findById(app.getAssignedKeyId()).orElse(null);
                    if (key == null) {
                        return MyApplicationResponse.masked(app, false, null);
                    }
                    String plain = keyCipher.decrypt(key.getKeyValueEnc());
                    // 본인 확인(reveal=true)일 때만 평문, 기본은 마스킹 (security.md)
                    String shown = reveal ? plain : KeyCipher.mask(plain);
                    return MyApplicationResponse.masked(app, true, shown);
                });
    }
}
