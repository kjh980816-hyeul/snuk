package com.chzikon.campaign.repository;

import com.chzikon.campaign.domain.CampaignApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long> {

    boolean existsByCampaignIdAndMemberId(Long campaignId, Long memberId);

    Optional<CampaignApplication> findByCampaignIdAndMemberId(Long campaignId, Long memberId);

    List<CampaignApplication> findByCampaignIdOrderByAppliedAtAsc(Long campaignId);

    List<CampaignApplication> findByMemberIdOrderByAppliedAtDesc(Long memberId);

    long countByCampaignIdAndStatus(Long campaignId, CampaignApplication.Status status);
}
