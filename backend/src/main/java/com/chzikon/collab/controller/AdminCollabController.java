package com.chzikon.collab.controller;

import com.chzikon.collab.dto.CollabDtos.*;
import com.chzikon.collab.service.CollabService;
import com.chzikon.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 콜라보/노출 어드민 CRUD + 순서. */
@RestController
@RequestMapping("/api/admin/collab")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCollabController {

    private final CollabService collabService;

    // ----- games -----
    @PostMapping("/games")
    public ResponseEntity<CollabGameResponse> createGame(@Valid @RequestBody CollabGameRequest req,
                                                         @AuthenticationPrincipal MemberPrincipal p) {
        return ResponseEntity.ok(CollabGameResponse.from(collabService.createGame(req, p.memberId())));
    }

    @PutMapping("/games/{id}")
    public ResponseEntity<CollabGameResponse> updateGame(@PathVariable Long id,
                                                         @Valid @RequestBody CollabGameRequest req,
                                                         @AuthenticationPrincipal MemberPrincipal p) {
        return ResponseEntity.ok(CollabGameResponse.from(collabService.updateGame(id, req, p.memberId())));
    }

    @DeleteMapping("/games/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal p) {
        collabService.deleteGame(id, p.memberId());
        return ResponseEntity.noContent().build();
    }

    // ----- videos -----
    @PostMapping("/videos")
    public ResponseEntity<ContentVideoResponse> createVideo(@Valid @RequestBody ContentVideoRequest req,
                                                            @AuthenticationPrincipal MemberPrincipal p) {
        return ResponseEntity.ok(ContentVideoResponse.from(collabService.createVideo(req, p.memberId())));
    }

    @PutMapping("/videos/{id}")
    public ResponseEntity<ContentVideoResponse> updateVideo(@PathVariable Long id,
                                                            @Valid @RequestBody ContentVideoRequest req,
                                                            @AuthenticationPrincipal MemberPrincipal p) {
        return ResponseEntity.ok(ContentVideoResponse.from(collabService.updateVideo(id, req, p.memberId())));
    }

    @DeleteMapping("/videos/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal p) {
        collabService.deleteVideo(id, p.memberId());
        return ResponseEntity.noContent().build();
    }

    // ----- clients -----
    @PostMapping("/clients")
    public ResponseEntity<ClientLogoResponse> createLogo(@Valid @RequestBody ClientLogoRequest req,
                                                         @AuthenticationPrincipal MemberPrincipal p) {
        return ResponseEntity.ok(ClientLogoResponse.from(collabService.createLogo(req, p.memberId())));
    }

    @PutMapping("/clients/{id}")
    public ResponseEntity<ClientLogoResponse> updateLogo(@PathVariable Long id,
                                                         @Valid @RequestBody ClientLogoRequest req,
                                                         @AuthenticationPrincipal MemberPrincipal p) {
        return ResponseEntity.ok(ClientLogoResponse.from(collabService.updateLogo(id, req, p.memberId())));
    }

    @DeleteMapping("/clients/{id}")
    public ResponseEntity<Void> deleteLogo(@PathVariable Long id, @AuthenticationPrincipal MemberPrincipal p) {
        collabService.deleteLogo(id, p.memberId());
        return ResponseEntity.noContent().build();
    }
}
