package com.chzikon.global.upload;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.member.domain.Member;
import com.chzikon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/** 스트리머 컨텐츠·대회 홍보 이미지 업로드 — STREAMER+ (어드민 업로드 엔드포인트는 ADMIN 전용이라 별도). */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class StreamerUploadController {

    private final FileStorageService fileStorage;
    private final MemberService memberService;

    @PostMapping("/image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal MemberPrincipal principal) {
        Member member = memberService.getById(principal.memberId());
        if (!member.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        return ResponseEntity.ok(Map.of("url", fileStorage.storeImage(file)));
    }
}
