package com.chzikon.spotlight.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.spotlight.dto.SpotlightDtos.SpotlightResponse;
import com.chzikon.spotlight.service.SpotlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 스포트라이트 운영 — /api/admin/** 은 SecurityConfig 에서 ADMIN 강제. */
@RestController
@RequestMapping("/api/admin/spotlights")
@RequiredArgsConstructor
public class AdminSpotlightController {

    private final SpotlightService spotlightService;

    @GetMapping
    public ResponseEntity<List<SpotlightResponse>> list() {
        return ResponseEntity.ok(spotlightService.findRecentForAdmin());
    }

    /** 승인 — 승인 시각부터 2시간 노출. */
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal MemberPrincipal principal) {
        spotlightService.approve(id, principal.memberId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        spotlightService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
