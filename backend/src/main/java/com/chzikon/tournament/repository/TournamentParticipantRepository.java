package com.chzikon.tournament.repository;

import com.chzikon.tournament.domain.TournamentParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {

    boolean existsByTournamentIdAndMemberId(Long tournamentId, Long memberId);

    Optional<TournamentParticipant> findByTournamentIdAndMemberId(Long tournamentId, Long memberId);

    List<TournamentParticipant> findByTournamentIdOrderByAppliedAtAsc(Long tournamentId);

    /** 마이페이지 집계용(MY 단계에서 사용). */
    List<TournamentParticipant> findByMemberIdOrderByAppliedAtDesc(Long memberId);
}
