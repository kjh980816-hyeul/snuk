package com.chzikon.campaign.repository;

import com.chzikon.campaign.domain.CampaignApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CampaignApplicationRepository extends JpaRepository<CampaignApplication, Long> {

    boolean existsByCampaignIdAndMemberId(Long campaignId, Long memberId);

    Optional<CampaignApplication> findByCampaignIdAndMemberId(Long campaignId, Long memberId);

    List<CampaignApplication> findByCampaignIdOrderByAppliedAtAsc(Long campaignId);

    List<CampaignApplication> findByMemberIdOrderByAppliedAtDesc(Long memberId);

    long countByCampaignIdAndStatus(Long campaignId, CampaignApplication.Status status);

    /** 후기 마감 경과 + 미경고 신청(스윕 대상, 후기 존재 여부는 서비스에서 확인). */
    List<CampaignApplication> findByStatusAndReviewDeadlineBeforeAndWarnedAtIsNull(
            CampaignApplication.Status status, LocalDateTime now);

    /** 경고 이력(어드민 경고 로그) — 최신 경고순. */
    List<CampaignApplication> findByWarnedAtIsNotNullOrderByWarnedAtDesc();
}
