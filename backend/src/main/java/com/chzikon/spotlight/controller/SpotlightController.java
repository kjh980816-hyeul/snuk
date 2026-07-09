package com.chzikon.spotlight.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.spotlight.dto.SpotlightDtos.SpotlightCreateRequest;
import com.chzikon.spotlight.dto.SpotlightDtos.SpotlightResponse;
import com.chzikon.spotlight.service.SpotlightService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spotlights")
@RequiredArgsConstructor
public class SpotlightController {

    private final SpotlightService spotlightService;

    /** 노출 중 스포트라이트(최대 2개) — 공개(사이드바). */
    @GetMapping("/active")
    public ResponseEntity<List<SpotlightResponse>> active() {
        return ResponseEntity.ok(spotlightService.findActive());
    }

    /** 등록 — STREAMER+ (서비스에서 권한·URL·중복 재검증). */
    @PostMapping
    public ResponseEntity<SpotlightResponse> create(@Valid @RequestBody SpotlightCreateRequest request,
                                                    @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(spotlightService.create(request, principal.memberId()));
    }
}
