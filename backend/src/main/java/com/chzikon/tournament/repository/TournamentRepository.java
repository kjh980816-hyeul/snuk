package com.chzikon.tournament.repository;

import com.chzikon.tournament.domain.Tournament;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    List<Tournament> findAllByOrderBySortOrderAscIdDesc();

    /** 승인 시 정원 차감용 비관적 쓰기 락(ADR-007). 마지막 정원 동시 승인 시 초과배정 방지. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tournament t where t.id = :id")
    Optional<Tournament> findByIdForUpdate(@Param("id") Long id);
}
