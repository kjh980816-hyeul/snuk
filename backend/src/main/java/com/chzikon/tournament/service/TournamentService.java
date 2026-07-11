package com.chzikon.tournament.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.tournament.domain.Tournament;
import com.chzikon.tournament.dto.TournamentCreateRequest;
import com.chzikon.tournament.dto.TournamentUpdateRequest;
import com.chzikon.tournament.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public List<Tournament> findAll() {
        return tournamentRepository.findAllByOrderBySortOrderAscIdDesc();
    }

    @Transactional(readOnly = true)
    public Tournament getById(Long id) {
        return tournamentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public Tournament create(TournamentCreateRequest req, Long actorId) {
        Tournament saved = tournamentRepository.save(req.toEntity());
        adminLogService.record(actorId, "TOURNAMENT_CREATE", "tournament", saved.getId(),
                "title=" + saved.getTitle());
        return saved;
    }

    @Transactional
    public Tournament update(Long id, TournamentUpdateRequest req, Long actorId) {
        Tournament tournament = getById(id);
        tournament.update(req.title(), req.description(), req.gameName(), req.bannerImageUrl(), req.detailImageUrl(),
                req.eventDate(), req.applyStart(), req.applyEnd(),
                req.capacity(), req.status(), req.resultText(), req.featured(), req.sortOrder());
        adminLogService.record(actorId, "TOURNAMENT_UPDATE", "tournament", id,
                "status=" + tournament.getStatus());
        return tournament;
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Tournament tournament = getById(id);
        tournamentRepository.delete(tournament);
        adminLogService.record(actorId, "TOURNAMENT_DELETE", "tournament", id,
                "title=" + tournament.getTitle());
    }
}
