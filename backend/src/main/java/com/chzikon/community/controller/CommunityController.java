package com.chzikon.community.controller;

import com.chzikon.community.dto.CommunityDtos.*;
import com.chzikon.community.service.CommunityService;
import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.global.upload.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 스눅 커뮤니티 — 조회는 공개(GET permitAll), 작성·댓글·버프·신고는 로그인 필요.
 * 수정/삭제 권한은 서비스에서 재검증(작성자 본인 + ADMIN).
 */
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final FileStorageService fileStorage;

    private static Long idOf(MemberPrincipal principal) {
        return principal != null ? principal.memberId() : null;
    }

    // ----- 게시판 -----

    @GetMapping("/boards")
    public ResponseEntity<List<BoardResponse>> boards(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.boards(idOf(principal)));
    }

    // ----- 목록/상세 -----

    @GetMapping("/posts")
    public ResponseEntity<PostPage> list(@RequestParam(required = false) Long boardId,
                                         @RequestParam(required = false, defaultValue = "new") String sort,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.list(boardId, sort, q, page, size, idOf(principal)));
    }

    @GetMapping("/posts/popular")
    public ResponseEntity<List<PostSummary>> popular(@RequestParam(required = false) Long boardId,
                                                     @RequestParam(defaultValue = "10") int limit,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.popular(boardId, limit, idOf(principal)));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<PostDetail> detail(@PathVariable Long id,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.get(id, idOf(principal)));
    }

    @GetMapping("/posts/{id}/nearby")
    public ResponseEntity<List<PostSummary>> nearby(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.nearby(id));
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<CommentResponse>> comments(@PathVariable Long id) {
        return ResponseEntity.ok(communityService.comments(id));
    }

    // ----- 작성/수정/삭제 (로그인) -----

    @PostMapping("/posts")
    public ResponseEntity<PostDetail> write(@Valid @RequestBody PostRequest req,
                                            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.write(principal.memberId(), req));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<PostDetail> edit(@PathVariable Long id,
                                           @Valid @RequestBody PostRequest req,
                                           @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.edit(id, principal.memberId(), req));
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        communityService.delete(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-posts")
    public ResponseEntity<List<PostSummary>> myPosts(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.myPosts(principal.memberId()));
    }

    // ----- 버프 / 댓글 / 신고 (로그인) -----

    @PostMapping("/posts/{id}/buff")
    public ResponseEntity<BuffResponse> buff(@PathVariable Long id,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.toggleBuff(id, principal.memberId()));
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long id,
                                                      @Valid @RequestBody CommentRequest req,
                                                      @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.addComment(id, principal.memberId(), req));
    }

    @DeleteMapping("/posts/{id}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id,
                                              @PathVariable Long commentId,
                                              @AuthenticationPrincipal MemberPrincipal principal) {
        communityService.deleteComment(id, commentId, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 공지 상세 — 사이드바 공지사항과 같은 데이터(뉴스는 기사 페이지 /news/{id} 로 이동). */
    @GetMapping("/notices/{id}")
    public ResponseEntity<PostDetail> noticeDetail(@PathVariable Long id,
                                                   @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(communityService.noticeDetail(id, idOf(principal)));
    }

    @DeleteMapping("/notices/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        communityService.deleteNotice(id, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    /** 글 본문 이미지 업로드 — 로그인 회원 누구나(글쓰기 권한은 게시판별로 서비스에서 재검증). */
    @PostMapping("/upload-image")
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(Map.of("url", fileStorage.storeImage(file)));
    }

    @PostMapping("/posts/{id}/report")
    public ResponseEntity<Void> report(@PathVariable Long id,
                                       @RequestBody(required = false) ReportRequest req,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        communityService.report(id, principal.memberId(), req);
        return ResponseEntity.noContent().build();
    }
}
