package com.chzikon.notice.controller;

import com.chzikon.notice.dto.NoticeDtos.NoticeResponse;
import com.chzikon.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    /** 공지 목록 — 공개(사이드바). 고정 우선 + 최신순. */
    @GetMapping
    public ResponseEntity<List<NoticeResponse>> list(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(noticeService.findLatest(limit).stream()
                .map(NoticeResponse::from).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(NoticeResponse.from(noticeService.getById(id)));
    }
}
