package com.chzikon.mypage.controller;

import com.chzikon.campaign.service.ReviewDeadlineService;
import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.mypage.dto.MypageSummaryResponse;
import com.chzikon.mypage.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

/** 마이페이지 — 로그인 필수(/api/** authenticated). 본인 데이터만 집계. */
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;
    private final ReviewDeadlineService reviewDeadlineService;

    @GetMapping("/summary")
    public ResponseEntity<MypageSummaryResponse> summary(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(mypageService.summary(principal.memberId()));
    }

    /** 후기 마감 7일 연장 — 게임당 1회, 본인 신청만(서비스 검증). */
    @PostMapping("/applications/{id}/extend")
    public ResponseEntity<Map<String, Object>> extendDeadline(
            @PathVariable Long id, @AuthenticationPrincipal MemberPrincipal principal) {
        LocalDateTime newDeadline = reviewDeadlineService.extend(id, principal.memberId());
        return ResponseEntity.ok(Map.of("applicationId", id, "reviewDeadline", newDeadline));
    }
}
