package com.chzikon.grant.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.grant.service.RoleRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 스트리머 권한 신청 (데모 '권한 신청' 메뉴). */
@RestController
@RequiredArgsConstructor
public class RoleRequestController {

    private final RoleRequestService service;

    /** 신청 — 로그인 필수(서비스에서 VIEWER 검증). */
    @PostMapping("/api/role-requests")
    public ResponseEntity<Map<String, Object>> apply(@RequestBody(required = false) Map<String, String> body,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        var request = service.apply(principal.memberId(), body != null ? body.get("message") : null);
        return ResponseEntity.ok(Map.of("requestId", request.getId(), "status", request.getStatus().name()));
    }

    /** 내 최근 신청 상태 — 없으면 204. */
    @GetMapping("/api/role-requests/me")
    public ResponseEntity<Map<String, Object>> mine(@AuthenticationPrincipal MemberPrincipal principal) {
        return service.myLatest(principal.memberId())
                .map(r -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("requestId", r.getId());
                    m.put("status", r.getStatus().name());
                    m.put("message", r.getMessage());
                    m.put("createdAt", r.getCreatedAt());
                    m.put("decidedAt", r.getDecidedAt());
                    return ResponseEntity.ok(m);
                })
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    // ---------- 어드민 ----------

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/admin/role-requests")
    public ResponseEntity<List<Map<String, Object>>> list() {
        return ResponseEntity.ok(service.listForAdmin());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/role-requests/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal MemberPrincipal principal) {
        service.approve(id, principal.memberId());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/role-requests/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        service.reject(id, principal.memberId());
        return ResponseEntity.ok().build();
    }
}
