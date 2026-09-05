package com.chzikon.campaign.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.Campaign;
import com.chzikon.campaign.domain.CampaignStatus;
import com.chzikon.campaign.dto.CampaignCreateRequest;
import com.chzikon.campaign.dto.CampaignUpdateRequest;
import com.chzikon.campaign.domain.CampaignApplication;
import com.chzikon.campaign.domain.GameKey;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.campaign.repository.GameKeyRepository;
import com.chzikon.collab.repository.CollabGameRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
import com.chzikon.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AdminLogService adminLogService;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final GameKeyRepository gameKeyRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final CollabGameRepository collabGameRepository;

    @Transactional(readOnly = true)
    public List<Campaign> findAll() {
        return campaignRepository.findAllByOrderBySortOrderAscIdDesc();
    }

    @Transactional(readOnly = true)
    public Campaign getById(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public Campaign create(CampaignCreateRequest req, Long actorId) {
        Campaign saved = campaignRepository.save(req.toEntity());
        adminLogService.record(actorId, "CAMPAIGN_CREATE", "campaign", saved.getId(),
                "title=" + saved.getTitle());
        return saved;
    }

    @Transactional
    public Campaign update(Long id, CampaignUpdateRequest req, Long actorId) {
        Campaign campaign = getById(id);
        campaign.update(req.title(), req.description(), req.gameName(), req.promoImageUrl(),
                req.eventDate(), req.applyStart(), req.applyEnd(), req.status(),
                req.distributionType(), req.keyMode(), req.totalSlots(), req.featured(), req.sortOrder(),
                com.chzikon.tournament.dto.ApplyFormJson.questionsToJson(req.applyQuestions()));
        adminLogService.record(actorId, "CAMPAIGN_UPDATE", "campaign", id,
                "status=" + campaign.getStatus());
        return campaign;
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Campaign campaign = getById(id);
        cascadeDelete(campaign);
        adminLogService.record(actorId, "CAMPAIGN_DELETE", "campaign", id, "title=" + campaign.getTitle());
    }

    /**
     * 컨텐츠 삭제 캐스케이드.
     * - 키가 이미 배정됐거나(ASSIGNED) 승인된 신청이 있으면 실제 가치가 나간 상태 → 409(M009)로 막고 '종료' 처리 안내.
     * - 그 외엔 미배정 키·대기/거절 신청을 함께 지우고, 연결된 게임체험단 게임은 링크만 해제(게임 유지).
     *   (기존엔 FK 때문에 키가 1개라도 있으면 500 — 2026-08-07 검증 잔여 버그)
     */
    private void cascadeDelete(Campaign campaign) {
        Long id = campaign.getId();
        boolean hasAssigned = gameKeyRepository.countByCampaignIdAndStatus(id, GameKey.Status.ASSIGNED) > 0
                || applicationRepository.countByCampaignIdAndStatus(id, CampaignApplication.Status.APPROVED) > 0;
        if (hasAssigned) {
            throw new BusinessException(ErrorCode.CAMPAIGN_HAS_ASSIGNMENTS);
        }
        gameKeyRepository.deleteByCampaignId(id);
        applicationRepository.deleteByCampaignId(id);
        collabGameRepository.findByCampaignId(id).forEach(g -> g.unlinkCampaign());
        campaignRepository.delete(campaign);
    }

    // ---------- 스트리머 본인 컨텐츠(항목 1) — STREAMER+ 등록, 본인 것만 수정/삭제 ----------

    @Transactional
    public Campaign createByStreamer(CampaignCreateRequest req, Long memberId) {
        Member member = memberService.getById(memberId);
        if (!member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        Campaign saved = campaignRepository.save(req.toEntity());
        saved.assignOwner(memberId);
        // 스트리머 등록분은 관리자 승인제: 무조건 준비중으로 시작(스눅 공식=어드민 등록은 그대로)
        if (member.getRole() != Role.ADMIN) saved.forcePreparing();
        adminLogService.record(memberId, "CAMPAIGN_CREATE_BY_STREAMER", "campaign", saved.getId(),
                "title=" + saved.getTitle() + " status=" + saved.getStatus());
        return saved;
    }

    /** 관리자 승인 — 스트리머 등록 컨텐츠를 모집중으로 전환 + 등록자에게 알림. */
    @Transactional
    public Campaign approve(Long id, Long adminId) {
        Campaign campaign = getById(id);
        if (campaign.getStatus() == CampaignStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 모집중인 컨텐츠입니다.");
        }
        campaign.approveRecruit();
        adminLogService.record(adminId, "CAMPAIGN_APPROVE", "campaign", id, "title=" + campaign.getTitle());
        if (campaign.getOwnerMemberId() != null) {
            notificationService.notify(campaign.getOwnerMemberId(), "CONTENT_APPROVED",
                    "컨텐츠가 승인됐어요 🎉",
                    "'" + campaign.getTitle() + "' 모집이 시작됐습니다. 이제 스트리머들이 신청할 수 있어요.",
                    "/campaigns");
        }
        return campaign;
    }

    @Transactional
    public Campaign updateOwned(Long id, CampaignUpdateRequest req, Long memberId) {
        Campaign campaign = requireOwnedOrAdmin(id, memberId);
        // 비관리자는 모집중/오픈예정으로 스스로 전환 불가(이미 승인돼 그 상태인 걸 그대로 저장하는 건 허용)
        if (req.status() != null && Campaign.isAdminOnlyStatus(req.status())
                && req.status() != campaign.getStatus()
                && memberService.getById(memberId).getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.CONTENT_NEEDS_APPROVAL);
        }
        campaign.update(req.title(), req.description(), req.gameName(), req.promoImageUrl(),
                req.eventDate(), req.applyStart(), req.applyEnd(), req.status(),
                req.distributionType(), req.keyMode(), req.totalSlots(), req.featured(), req.sortOrder(),
                com.chzikon.tournament.dto.ApplyFormJson.questionsToJson(req.applyQuestions()));
        return campaign;
    }

    @Transactional
    public void deleteOwned(Long id, Long memberId) {
        Campaign campaign = requireOwnedOrAdmin(id, memberId);
        cascadeDelete(campaign);
        adminLogService.record(memberId, "CAMPAIGN_DELETE", "campaign", id, "title=" + campaign.getTitle());
    }

    private Campaign requireOwnedOrAdmin(Long id, Long memberId) {
        Campaign campaign = getById(id);
        Member member = memberService.getById(memberId);
        if (!campaign.isOwnedBy(memberId) && member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return campaign;
    }
}
