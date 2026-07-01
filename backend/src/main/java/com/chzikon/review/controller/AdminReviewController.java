package com.chzikon.review.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /** 후기 노출/숨김. body: {"hidden": true|false} (기본 숨김 처리). */
    @PostMapping("/{id}/hide")
    public ResponseEntity<Void> hide(@PathVariable Long id,
                                     @RequestBody(required = false) Map<String, Boolean> body,
                                     @AuthenticationPrincipal MemberPrincipal principal) {
        boolean hidden = body == null || body.getOrDefault("hidden", true);
        reviewService.setHidden(id, hidden, principal.memberId());
        return ResponseEntity.ok().build();
    }
}
