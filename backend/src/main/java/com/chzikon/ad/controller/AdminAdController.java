package com.chzikon.ad.controller;

import com.chzikon.ad.dto.AdSlotDtos.AdSlotRequest;
import com.chzikon.ad.dto.AdSlotDtos.AdSlotResponse;
import com.chzikon.ad.service.AdSlotService;
import com.chzikon.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 광고 슬롯 CRUD — ADMIN 전용(SecurityConfig /api/admin/** + @PreAuthorize 2중화). */
@RestController
@RequestMapping("/api/admin/ads")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminAdController {

    private final AdSlotService adSlotService;

    @GetMapping
    public ResponseEntity<List<AdSlotResponse>> list() {
        return ResponseEntity.ok(adSlotService.findAll().stream().map(AdSlotResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<AdSlotResponse> create(@Valid @RequestBody AdSlotRequest req,
                                                 @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(AdSlotResponse.from(adSlotService.create(req, principal.memberId())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdSlotResponse> update(@PathVariable Long id, @Valid @RequestBody AdSlotRequest req,
                                                 @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(AdSlotResponse.from(adSlotService.update(id, req, principal.memberId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal principal) {
        adSlotService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
