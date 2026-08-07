package com.chzikon.community.controller;

import com.chzikon.community.dto.CommunityDtos.*;
import com.chzikon.community.service.CommunityService;
import com.chzikon.global.security.MemberPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 커뮤니티 운영 — 게시판 관리 / 글 숨김·삭제 / 신고함. (/api/admin/** = ADMIN 전용, 서비스에서도 재검증) */
@RestController
@RequestMapping("/api/admin/community")
@RequiredArgsConstructor
public class AdminCommunityController {

    private final CommunityService communityService;

    @GetMapping("/boards")
    public ResponseEntity<List<BoardResponse>> boards() {
        return ResponseEntity.ok(communityService.allBoards());
    }

    @PostMapping("/boards")
    public ResponseEntity<BoardResponse> createBoard(@Valid @RequestBody BoardRequest req) {
        return ResponseEntity.ok(communityService.createBoard(req));
    }

    @PutMapping("/boards/{id}")
    public ResponseEntity<BoardResponse> updateBoard(@PathVariable Long id,
                                                     @Valid @RequestBody BoardRequest req) {
        return ResponseEntity.ok(communityService.updateBoard(id, req));
    }

    @DeleteMapping("/boards/{id}")
    public ResponseEntity<Void> deleteBoard(@PathVariable Long id) {
        communityService.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/posts")
    public ResponseEntity<List<PostSummary>> posts() {
        return ResponseEntity.ok(communityService.recentForAdmin());
    }

    @PatchMapping("/posts/{id}/hidden")
    public ResponseEntity<PostSummary> setHidden(@PathVariable Long id,
                                                 @RequestBody Map<String, Boolean> body,
                                                 @AuthenticationPrincipal MemberPrincipal principal) {
        boolean hidden = Boolean.TRUE.equals(body.get("hidden"));
        return ResponseEntity.ok(communityService.setHidden(id, principal.memberId(), hidden));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id,
                                           @AuthenticationPrincipal MemberPrincipal principal) {
        communityService.deleteAsAdmin(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reports")
    public ResponseEntity<List<ReportView>> reports() {
        return ResponseEntity.ok(communityService.reports());
    }

    @DeleteMapping("/reports/{id}")
    public ResponseEntity<Void> dismissReport(@PathVariable Long id) {
        communityService.dismissReport(id);
        return ResponseEntity.noContent().build();
    }
}
