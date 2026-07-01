package com.chzikon.campaign.repository;

import com.chzikon.campaign.domain.Campaign;
import com.chzikon.campaign.domain.CampaignStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    List<Campaign> findAllByOrderBySortOrderAscIdDesc();

    List<Campaign> findByStatusOrderBySortOrderAscIdDesc(CampaignStatus status);

    List<Campaign> findByFeaturedTrueAndStatusOrderBySortOrderAsc(CampaignStatus status);

    /**
     * FCFS 슬롯 차감용 비관적 쓰기 락(ADR-007). 마지막 슬롯 동시 클릭 시 초과배정 방지.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Campaign c where c.id = :id")
    Optional<Campaign> findByIdForUpdate(@Param("id") Long id);
}
