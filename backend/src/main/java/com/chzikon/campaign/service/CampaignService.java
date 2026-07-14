package com.chzikon.campaign.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.Campaign;
import com.chzikon.campaign.dto.CampaignCreateRequest;
import com.chzikon.campaign.dto.CampaignUpdateRequest;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
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
                req.distributionType(), req.keyMode(), req.totalSlots(), req.featured(), req.sortOrder());
        adminLogService.record(actorId, "CAMPAIGN_UPDATE", "campaign", id,
                "status=" + campaign.getStatus());
        return campaign;
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Campaign campaign = getById(id);
        campaignRepository.delete(campaign);
        adminLogService.record(actorId, "CAMPAIGN_DELETE", "campaign", id, "title=" + campaign.getTitle());
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
        adminLogService.record(memberId, "CAMPAIGN_CREATE_BY_STREAMER", "campaign", saved.getId(),
                "title=" + saved.getTitle());
        return saved;
    }

    @Transactional
    public Campaign updateOwned(Long id, CampaignUpdateRequest req, Long memberId) {
        Campaign campaign = requireOwnedOrAdmin(id, memberId);
        campaign.update(req.title(), req.description(), req.gameName(), req.promoImageUrl(),
                req.eventDate(), req.applyStart(), req.applyEnd(), req.status(),
                req.distributionType(), req.keyMode(), req.totalSlots(), req.featured(), req.sortOrder());
        return campaign;
    }

    @Transactional
    public void deleteOwned(Long id, Long memberId) {
        Campaign campaign = requireOwnedOrAdmin(id, memberId);
        campaignRepository.delete(campaign);
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
