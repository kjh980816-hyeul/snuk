package com.chzikon.campaign.repository;

import com.chzikon.campaign.domain.GameKey;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface GameKeyRepository extends JpaRepository<GameKey, Long> {

    List<GameKey> findByCampaignIdOrderByIdAsc(Long campaignId);

    long countByCampaignIdAndStatus(Long campaignId, GameKey.Status status);

    /** 캠페인 내 기존 fingerprint 집합(중복 일괄등록 감지용). */
    @Query("select gk.keyFingerprint from GameKey gk where gk.campaignId = :campaignId")
    Set<String> findFingerprints(@Param("campaignId") Long campaignId);

    /**
     * 가용 키를 비관적 락으로 선점(FCFS 키 배정). 동시 배정 중복 방지(ADR-007).
     * status 는 파라미터로 전달(JPQL 인라인 enum 회피).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select gk from GameKey gk where gk.campaignId = :campaignId and gk.status = :status order by gk.id asc")
    List<GameKey> findAvailableForUpdate(@Param("campaignId") Long campaignId,
                                         @Param("status") GameKey.Status status,
                                         Pageable pageable);

    Optional<GameKey> findByIdAndCampaignId(Long id, Long campaignId);
}
