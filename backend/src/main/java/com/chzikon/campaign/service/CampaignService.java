package com.chzikon.campaign.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.Campaign;
import com.chzikon.campaign.dto.CampaignCreateRequest;
import com.chzikon.campaign.dto.CampaignUpdateRequest;
import com.chzikon.campaign.repository.CampaignRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final AdminLogService adminLogService;

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
}
