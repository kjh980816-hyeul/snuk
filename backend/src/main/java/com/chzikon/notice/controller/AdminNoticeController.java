package com.chzikon.notice.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.notice.dto.NoticeDtos.NoticeRequest;
import com.chzikon.notice.dto.NoticeDtos.NoticeResponse;
import com.chzikon.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 공지 CRUD — /api/admin/** 은 SecurityConfig 에서 ADMIN 강제. */
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<NoticeResponse> create(@Valid @RequestBody NoticeRequest request,
                                                 @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(NoticeResponse.from(noticeService.create(request, principal.memberId())));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NoticeResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody NoticeRequest request,
                                                 @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(NoticeResponse.from(noticeService.update(id, request, principal.memberId())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        noticeService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
