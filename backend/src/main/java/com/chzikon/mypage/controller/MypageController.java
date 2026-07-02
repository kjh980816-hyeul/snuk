package com.chzikon.mypage.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.mypage.dto.MypageSummaryResponse;
import com.chzikon.mypage.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지 — 로그인 필수(/api/** authenticated). 본인 데이터만 집계. */
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping("/summary")
    public ResponseEntity<MypageSummaryResponse> summary(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(mypageService.summary(principal.memberId()));
    }
}
