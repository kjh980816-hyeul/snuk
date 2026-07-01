package com.chzikon.campaign.controller;

import com.chzikon.campaign.dto.*;
import com.chzikon.campaign.service.CampaignApplicationService;
import com.chzikon.campaign.service.CampaignService;
import com.chzikon.campaign.service.GameKeyService;
import com.chzikon.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 어드민 캠페인 운영. 클래스 단위 ADMIN 강제(2중화: SecurityConfig + @PreAuthorize). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCampaignController {

    private final CampaignService campaignService;
    private final GameKeyService gameKeyService;
    private final CampaignApplicationService applicationService;

    // ----- 캠페인 CRUD -----
    @PostMapping("/campaigns")
    public ResponseEntity<CampaignResponse> create(@Valid @RequestBody CampaignCreateRequest req,
                                                   @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.create(req, principal.memberId())));
    }

    @PutMapping("/campaigns/{id}")
    public ResponseEntity<CampaignResponse> update(@PathVariable Long id,
                                                   @RequestBody CampaignUpdateRequest req,
                                                   @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.update(id, req, principal.memberId())));
    }

    @DeleteMapping("/campaigns/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        campaignService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ----- 게임 키 -----
    @PostMapping("/campaigns/{id}/keys")
    public ResponseEntity<KeyRegisterResult> registerKeys(@PathVariable Long id,
                                                          @Valid @RequestBody KeyRegisterRequest req,
                                                          @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(gameKeyService.bulkRegister(id, req.rawKeys(), principal.memberId()));
    }

    @GetMapping("/campaigns/{id}/keys")
    public ResponseEntity<List<GameKeyAdminView>> listKeys(@PathVariable Long id) {
        return ResponseEntity.ok(gameKeyService.listMasked(id));
    }

    @DeleteMapping("/campaigns/{id}/keys/{keyId}")
    public ResponseEntity<Void> deleteKey(@PathVariable Long id, @PathVariable Long keyId,
                                          @AuthenticationPrincipal MemberPrincipal principal) {
        gameKeyService.deleteBeforeAssign(id, keyId, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/campaigns/{id}/keys/{keyId}/revoke")
    public ResponseEntity<Void> revokeKey(@PathVariable Long id, @PathVariable Long keyId,
                                          @AuthenticationPrincipal MemberPrincipal principal) {
        gameKeyService.revoke(id, keyId, principal.memberId());
        return ResponseEntity.ok().build();
    }

    // ----- 신청자 / 승인제 배정 -----
    @GetMapping("/campaigns/{id}/applications")
    public ResponseEntity<List<ApplicationAdminView>> applications(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.listApplications(id));
    }

    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id,
                                        @AuthenticationPrincipal MemberPrincipal principal) {
        applicationService.approve(id, principal.memberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        applicationService.reject(id, principal.memberId());
        return ResponseEntity.ok().build();
    }
}
