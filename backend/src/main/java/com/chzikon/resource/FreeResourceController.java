package com.chzikon.resource;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.global.upload.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/** 무료소스 자료실(V15 항목 19) — 목록/다운로드는 공개, 등록/삭제는 ADMIN. */
@RestController
@RequiredArgsConstructor
public class FreeResourceController {

    private final FreeResourceRepository repository;
    private final FileStorageService fileStorage;

    public record ResourceView(Long id, String title, String description,
                               String fileUrl, String imageUrl, java.time.LocalDateTime createdAt) {
        static ResourceView from(FreeResource r) {
            return new ResourceView(r.getId(), r.getTitle(), r.getDescription(),
                    r.getFileUrl(), r.getImageUrl(), r.getCreatedAt());
        }
    }

    /** 공개 목록. */
    @GetMapping("/api/resources")
    public ResponseEntity<List<ResourceView>> list() {
        return ResponseEntity.ok(repository.findAllByOrderByCreatedAtDesc().stream()
                .map(ResourceView::from).toList());
    }

    /** 어드민 등록 — 링크(주소) 또는 파일 중 하나 필수 + title/description + 썸네일(선택). */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/admin/resources")
    public ResponseEntity<ResourceView> create(@RequestParam("title") String title,
                                               @RequestParam(value = "description", required = false) String description,
                                               @RequestParam(value = "link", required = false) String link,
                                               @RequestParam(value = "file", required = false) MultipartFile file,
                                               @RequestParam(value = "image", required = false) MultipartFile image,
                                               @AuthenticationPrincipal MemberPrincipal principal) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "제목을 입력해주세요.");
        }
        String fileUrl;
        if (link != null && !link.isBlank()) {
            String trimmed = link.trim();
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "링크는 http:// 또는 https:// 로 시작해야 합니다.");
            }
            fileUrl = trimmed;
        } else if (file != null && !file.isEmpty()) {
            fileUrl = fileStorage.storeResourceFile(file);
        } else {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "링크(주소) 또는 파일 중 하나는 입력해야 합니다.");
        }
        String imageUrl = (image != null && !image.isEmpty()) ? fileStorage.storeImage(image) : null;
        FreeResource saved = repository.save(
                new FreeResource(title.trim(), description, fileUrl, imageUrl, principal.memberId()));
        return ResponseEntity.ok(ResourceView.from(saved));
    }

    /** 어드민 수정(텍스트만). */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/admin/resources/{id}")
    public ResponseEntity<ResourceView> update(@PathVariable Long id,
                                               @RequestBody Map<String, String> body) {
        FreeResource resource = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        resource.update(body.get("title"), body.get("description"), null, resource.getImageUrl());
        repository.save(resource);
        return ResponseEntity.ok(ResourceView.from(resource));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/api/admin/resources/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.findById(id).ifPresent(r -> {
            fileStorage.deleteIfLocal(r.getFileUrl());
            fileStorage.deleteIfLocal(r.getImageUrl());
            repository.delete(r);
        });
        return ResponseEntity.noContent().build();
    }
}
