package com.chzikon.tournament.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
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
    private final MemberService memberService;

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
                req.capacity(), req.status(), req.resultText(),
                com.chzikon.tournament.dto.ApplyFormJson.questionsToJson(req.applyQuestions()),
                req.featured(), req.sortOrder());
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

    // ---------- 스트리머 본인 대회 — STREAMER+ 등록, 본인 것만 수정/삭제 (featured=공식 슬롯은 관리자 전용) ----------

    @Transactional
    public Tournament createByStreamer(TournamentCreateRequest req, Long memberId) {
        Member member = memberService.getById(memberId);
        if (!member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        Tournament saved = tournamentRepository.save(req.toEntity());
        saved.assignOwner(memberId);
        adminLogService.record(memberId, "TOURNAMENT_CREATE_BY_STREAMER", "tournament", saved.getId(),
                "title=" + saved.getTitle());
        return saved;
    }

    @Transactional
    public Tournament updateOwned(Long id, TournamentUpdateRequest req, Long memberId) {
        Tournament tournament = getById(id);
        Member member = requireOwnerOrAdmin(tournament, memberId);
        boolean isAdmin = member.getRole() == Role.ADMIN;
        tournament.update(req.title(), req.description(), req.gameName(), req.bannerImageUrl(), req.detailImageUrl(),
                req.eventDate(), req.applyStart(), req.applyEnd(),
                req.capacity(), req.status(), req.resultText(),
                com.chzikon.tournament.dto.ApplyFormJson.questionsToJson(req.applyQuestions()),
                isAdmin ? req.featured() : null, req.sortOrder());
        return tournament;
    }

    @Transactional
    public void deleteOwned(Long id, Long memberId) {
        Tournament tournament = getById(id);
        requireOwnerOrAdmin(tournament, memberId);
        tournamentRepository.delete(tournament);
        adminLogService.record(memberId, "TOURNAMENT_DELETE", "tournament", id,
                "title=" + tournament.getTitle());
    }

    private Member requireOwnerOrAdmin(Tournament tournament, Long memberId) {
        Member member = memberService.getById(memberId);
        if (!tournament.isOwnedBy(memberId) && member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return member;
    }
}
