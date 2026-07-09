package com.chzikon.spotlight.repository;

import com.chzikon.spotlight.domain.Spotlight;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SpotlightRepository extends JpaRepository<Spotlight, Long> {

    /** 노출 중(미만료) 최신순. */
    @Query("SELECT s FROM Spotlight s WHERE s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Spotlight> findActive(@Param("now") LocalDateTime now, Pageable pageable);

    boolean existsByMemberIdAndExpiresAtAfter(Long memberId, LocalDateTime now);

    /** 어드민 목록 — 최근 등록순. */
    List<Spotlight> findTop50ByOrderByCreatedAtDesc();
}
