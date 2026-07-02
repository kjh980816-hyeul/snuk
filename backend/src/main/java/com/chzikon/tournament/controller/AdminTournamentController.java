package com.chzikon.tournament.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.tournament.dto.*;
import com.chzikon.tournament.service.TournamentParticipantService;
import com.chzikon.tournament.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 어드민 대회 운영. 클래스 단위 ADMIN 강제(2중화: SecurityConfig + @PreAuthorize). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTournamentController {

    private final TournamentService tournamentService;
    private final TournamentParticipantService participantService;

    // ----- 대회 CRUD (결과입력은 update 의 resultText) -----
    @PostMapping("/tournaments")
    public ResponseEntity<TournamentResponse> create(@Valid @RequestBody TournamentCreateRequest req,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(TournamentResponse.from(tournamentService.create(req, principal.memberId())));
    }

    @PutMapping("/tournaments/{id}")
    public ResponseEntity<TournamentResponse> update(@PathVariable Long id,
                                                     @RequestBody TournamentUpdateRequest req,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(TournamentResponse.from(tournamentService.update(id, req, principal.memberId())));
    }

    @DeleteMapping("/tournaments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        tournamentService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ----- 참가 신청 승인/거절 -----
    @GetMapping("/tournaments/{id}/participants")
    public ResponseEntity<List<ParticipantAdminView>> participants(@PathVariable Long id) {
        return ResponseEntity.ok(participantService.listParticipants(id));
    }

    @PostMapping("/participants/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal MemberPrincipal principal) {
        participantService.approve(id, principal.memberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/participants/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        participantService.reject(id, principal.memberId());
        return ResponseEntity.ok().build();
    }
}
