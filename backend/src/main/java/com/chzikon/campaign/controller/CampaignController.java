package com.chzikon.campaign.controller;

import com.chzikon.campaign.dto.CampaignCreateRequest;
import com.chzikon.campaign.dto.CampaignResponse;
import com.chzikon.campaign.dto.CampaignUpdateRequest;
import com.chzikon.campaign.dto.MyApplicationResponse;
import jakarta.validation.Valid;
import com.chzikon.campaign.service.CampaignApplicationService;
import com.chzikon.campaign.service.CampaignService;
import com.chzikon.global.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignApplicationService applicationService;

    /** 홈/목록 — 공개. */
    @GetMapping
    public ResponseEntity<List<CampaignResponse>> list() {
        return ResponseEntity.ok(campaignService.findAll().stream()
                .map(CampaignResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(CampaignResponse.from(campaignService.getById(id)));
    }

    /** 컨텐츠 등록 — STREAMER+ (항목 1, 서비스 재검증). 소유자 기록되어 본인 수정/삭제 가능. */
    @PostMapping
    public ResponseEntity<CampaignResponse> create(
            @Valid @RequestBody CampaignCreateRequest req,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(CampaignResponse.from(
                campaignService.createByStreamer(req, principal.memberId())));
    }

    /** 본인 컨텐츠 수정 — 소유자 또는 ADMIN. */
    @PutMapping("/{id}")
    public ResponseEntity<CampaignResponse> update(
            @PathVariable Long id,
            @RequestBody CampaignUpdateRequest req,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(CampaignResponse.from(
                campaignService.updateOwned(id, req, principal.memberId())));
    }

    /** 본인 컨텐츠 삭제 — 소유자 또는 ADMIN. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        campaignService.deleteOwned(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 신청 — STREAMER+ (서비스에서 권한·슬롯·중복 백엔드 재검증). */
    @PostMapping("/{id}/apply")
    public ResponseEntity<Map<String, Object>> apply(@PathVariable Long id,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        var application = applicationService.apply(id, principal.memberId());
        return ResponseEntity.ok(Map.of(
                "applicationId", application.getId(),
                "status", application.getStatus().name()));
    }

    /** 내 신청 상태/배정 키 — 본인만. reveal=true 면 평문 키 노출(본인 확인). */
    @GetMapping("/{id}/my-application")
    public ResponseEntity<MyApplicationResponse> myApplication(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") boolean reveal,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return applicationService.getMyApplication(id, principal.memberId(), reveal)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
