package com.chzikon.review.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.review.dto.ReviewCreateRequest;
import com.chzikon.review.dto.ReviewResponse;
import com.chzikon.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** 캠페인 후기 목록 — 공개(숨김 제외). */
    @GetMapping("/api/campaigns/{id}/reviews")
    public ResponseEntity<List<ReviewResponse>> byCampaign(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.listByCampaign(id).stream()
                .map(ReviewResponse::from).toList());
    }

    /** 후기 작성 — 배정 참가자만(서비스 검증). */
    @PostMapping("/api/campaigns/{id}/reviews")
    public ResponseEntity<ReviewResponse> write(@PathVariable Long id,
                                                @Valid @RequestBody ReviewCreateRequest req,
                                                @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(ReviewResponse.from(reviewService.write(id, principal.memberId(), req)));
    }

    /** 본인 후기 수정. */
    @PutMapping("/api/reviews/{id}")
    public ResponseEntity<ReviewResponse> edit(@PathVariable Long id,
                                               @Valid @RequestBody ReviewCreateRequest req,
                                               @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(ReviewResponse.from(reviewService.edit(id, principal.memberId(), req)));
    }

    /** 전체 후기 모음 — 공개(콜라보 섹션용). */
    @GetMapping("/api/reviews")
    public ResponseEntity<List<ReviewResponse>> all() {
        return ResponseEntity.ok(reviewService.listAll().stream()
                .map(ReviewResponse::from).toList());
    }
}
