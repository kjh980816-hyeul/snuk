package com.chzikon.tournament.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.tournament.dto.MyParticipationResponse;
import com.chzikon.tournament.dto.ParticipantPublicView;
import com.chzikon.tournament.dto.TournamentCreateRequest;
import com.chzikon.tournament.dto.TournamentResponse;
import com.chzikon.tournament.dto.TournamentUpdateRequest;
import jakarta.validation.Valid;
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

    /** 대회 등록 — STREAMER+ (서비스 재검증). 소유자 기록, featured(공식 슬롯)는 관리자 전용. */
    @PostMapping
    public ResponseEntity<TournamentResponse> create(
            @Valid @RequestBody TournamentCreateRequest req,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(TournamentResponse.from(
                tournamentService.createByStreamer(req, principal.memberId())));
    }

    /** 본인 대회 수정 — 소유자 또는 ADMIN. */
    @PutMapping("/{id}")
    public ResponseEntity<TournamentResponse> update(
            @PathVariable Long id,
            @RequestBody TournamentUpdateRequest req,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(TournamentResponse.from(
                tournamentService.updateOwned(id, req, principal.memberId())));
    }

    /** 본인 대회 삭제 — 소유자 또는 ADMIN. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        tournamentService.deleteOwned(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 참가 신청 바디 — 주최자 질문 답변(텍스트+사진, 질문 없으면 생략 가능). */
    public record ApplyRequest(List<com.chzikon.tournament.dto.ApplyFormJson.ApplyAnswer> answers) {
    }

    /** 참가 신청 — STREAMER+ (서비스에서 권한·상태·중복·답변 백엔드 재검증). */
    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable Long id,
                                                     @RequestBody(required = false) ApplyRequest req,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        var participant = participantService.apply(id, principal.memberId(),
                req != null ? req.answers() : null);
        return ResponseEntity.ok(Map.of(
                "participantId", participant.getId(),
                "status", participant.getStatus().name()));
    }

    /** 참가 신청 내역 CSV 다운로드(엑셀 호환, BOM 포함) — 소유 스트리머 또는 ADMIN (항목 18). */
    @GetMapping("/{id}/participants/export")
    public ResponseEntity<byte[]> exportParticipants(@PathVariable Long id,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        String csv = "﻿" + participantService.exportCsv(id, principal.memberId());
        byte[] body = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv; charset=UTF-8")
                .header("Content-Disposition", "attachment; filename=\"tournament-" + id + "-participants.csv\"")
                .body(body);
    }

    /** 참가자 관리 목록 — 대회 소유 스트리머 또는 ADMIN. */
    @GetMapping("/{id}/participants/manage")
    public ResponseEntity<List<com.chzikon.tournament.dto.ParticipantManageView>> manageList(
            @PathVariable Long id,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(participantService.listForManage(id, principal.memberId()));
    }

    /** 참가 승인 — 대회 소유 스트리머 또는 ADMIN (정원 원자 차감). */
    @PostMapping("/{id}/participants/{pid}/approve")
    public ResponseEntity<Void> approveParticipant(@PathVariable Long id, @PathVariable Long pid,
                                                   @AuthenticationPrincipal MemberPrincipal principal) {
        participantService.decideByOwner(id, pid, true, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 참가 거절 — 대회 소유 스트리머 또는 ADMIN. */
    @PostMapping("/{id}/participants/{pid}/reject")
    public ResponseEntity<Void> rejectParticipant(@PathVariable Long id, @PathVariable Long pid,
                                                  @AuthenticationPrincipal MemberPrincipal principal) {
        participantService.decideByOwner(id, pid, false, principal.memberId());
        return ResponseEntity.noContent().build();
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
