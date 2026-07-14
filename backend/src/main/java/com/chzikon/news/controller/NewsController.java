package com.chzikon.news.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.global.upload.FileStorageService;
import com.chzikon.news.dto.NewsDtos.NewsCreateRequest;
import com.chzikon.news.dto.NewsDtos.NewsResponse;
import com.chzikon.news.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 스눅 뉴스 — 조회 공개(GET permitAll), 작성/수정/삭제는 REPORTER+(서비스 재검증). */
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final FileStorageService fileStorage;

    @GetMapping
    public ResponseEntity<List<NewsResponse>> list() {
        return ResponseEntity.ok(newsService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(newsService.get(id));
    }

    @PostMapping
    public ResponseEntity<NewsResponse> write(@Valid @RequestBody NewsCreateRequest req,
                                              @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(newsService.write(principal.memberId(), req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<NewsResponse> edit(@PathVariable Long id,
                                             @Valid @RequestBody NewsCreateRequest req,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(newsService.edit(id, principal.memberId(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        newsService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 기사 썸네일 업로드 — REPORTER+ (어드민 업로드 엔드포인트는 ADMIN 전용이라 별도). */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal MemberPrincipal principal) {
        newsService.assertReporter(principal.memberId());
        return ResponseEntity.ok(Map.of("url", fileStorage.storeImage(file)));
    }
}
