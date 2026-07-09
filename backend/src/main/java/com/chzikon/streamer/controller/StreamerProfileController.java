package com.chzikon.streamer.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.streamer.dto.StreamerDtos.StreamerPostRequest;
import com.chzikon.streamer.dto.StreamerDtos.StreamerPostResponse;
import com.chzikon.streamer.dto.StreamerDtos.StreamerProfileResponse;
import com.chzikon.streamer.service.StreamerProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StreamerProfileController {

    private final StreamerProfileService service;

    /** 프로필 — 공개(로그인 시 following 포함). */
    @GetMapping("/api/streamers/{id}")
    public ResponseEntity<StreamerProfileResponse> profile(@PathVariable Long id,
                                                           @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(service.getProfile(id, principal != null ? principal.memberId() : null));
    }

    /** 팔로우 — 로그인 필요. */
    @PostMapping("/api/streamers/{id}/follow")
    public ResponseEntity<Map<String, Object>> follow(@PathVariable Long id,
                                                      @AuthenticationPrincipal MemberPrincipal principal) {
        long count = service.follow(id, principal.memberId());
        return ResponseEntity.ok(Map.of("following", true, "followCount", count));
    }

    @DeleteMapping("/api/streamers/{id}/follow")
    public ResponseEntity<Map<String, Object>> unfollow(@PathVariable Long id,
                                                        @AuthenticationPrincipal MemberPrincipal principal) {
        long count = service.unfollow(id, principal.memberId());
        return ResponseEntity.ok(Map.of("following", false, "followCount", count));
    }

    /** 개인 게시판 목록 — 공개. */
    @GetMapping("/api/streamers/{id}/posts")
    public ResponseEntity<List<StreamerPostResponse>> posts(@PathVariable Long id,
                                                            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(service.listPosts(id, principal != null ? principal.memberId() : null));
    }

    /** 글 작성 — 로그인 회원 누구나. */
    @PostMapping("/api/streamers/{id}/posts")
    public ResponseEntity<StreamerPostResponse> write(@PathVariable Long id,
                                                      @Valid @RequestBody StreamerPostRequest request,
                                                      @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(service.writePost(id, principal.memberId(), request));
    }

    /** 글 삭제 — 작성자 본인 + 해당 스트리머 + ADMIN (서비스 강제). */
    @DeleteMapping("/api/streamer-posts/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId,
                                       @AuthenticationPrincipal MemberPrincipal principal) {
        service.deletePost(postId, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
