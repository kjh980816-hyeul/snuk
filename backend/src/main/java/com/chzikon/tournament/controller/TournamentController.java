package com.chzikon.tournament.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.tournament.dto.MyParticipationResponse;
import com.chzikon.tournament.dto.ParticipantPublicView;
import com.chzikon.tournament.dto.TournamentResponse;
import com.chzikon.tournament.service.TournamentParticipantService;
import com.chzikon.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentParticipantService participantService;

    /** 대회 목록 — 공개. */
    @GetMapping
    public ResponseEntity<List<TournamentResponse>> list() {
        return ResponseEntity.ok(tournamentService.findAll().stream()
                .map(TournamentResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TournamentResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(TournamentResponse.from(tournamentService.getById(id)));
    }

    /** 공개 로스터 — 승인된 참가자(닉네임·프사)만 노출. */
    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantPublicView>> participants(@PathVariable Long id) {
        return ResponseEntity.ok(participantService.listApprovedPublic(id));
    }

    /** 참가 신청 — STREAMER+ (서비스에서 권한·상태·중복 백엔드 재검증). */
    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable Long id,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        var participant = participantService.apply(id, principal.memberId());
        return ResponseEntity.ok(Map.of(
                "participantId", participant.getId(),
                "status", participant.getStatus().name()));
    }

    /** 내 참가 신청 상태 — 본인만. */
    @GetMapping("/{id}/my-participation")
    public ResponseEntity<MyParticipationResponse> myParticipation(
            @PathVariable Long id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return participantService.getMyParticipation(id, principal.memberId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
