package com.chzikon.spotlight.repository;

import com.chzikon.spotlight.domain.Spotlight;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpotlightRepository extends JpaRepository<Spotlight, Long> {

    /** 노출 중(승인됨+미만료+예정시각 도달) 최신순 — 예정 일시가 있으면 그 시각부터 자동 노출. */
    @Query("SELECT s FROM Spotlight s WHERE s.approved = true AND s.expiresAt > :now"
            + " AND (s.scheduledAt IS NULL OR s.scheduledAt <= :now) ORDER BY s.approvedAt DESC")
    List<Spotlight> findActive(@Param("now") LocalDateTime now, Pageable pageable);

    boolean existsByMemberIdAndExpiresAtAfter(Long memberId, LocalDateTime now);

    /** 어드민 목록 — 최근 등록순. */
    List<Spotlight> findTop50ByOrderByCreatedAtDesc();
}
