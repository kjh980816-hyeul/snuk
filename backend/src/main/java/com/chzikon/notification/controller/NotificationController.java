package com.chzikon.notification.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 회원 알림함 — 본인 것만(로그인 필수). */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(@AuthenticationPrincipal MemberPrincipal principal) {
        List<Map<String, Object>> items = service.listMine(principal.memberId()).stream()
                .map(n -> {
                    java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", n.getId());
                    m.put("type", n.getType());
                    m.put("title", n.getTitle());
                    m.put("body", n.getBody());
                    m.put("linkPath", n.getLinkPath());
                    m.put("read", !n.isUnread());
                    m.put("createdAt", n.getCreatedAt());
                    return m;
                })
                .toList();
        return ResponseEntity.ok(Map.of(
                "unreadCount", service.unreadCount(principal.memberId()),
                "items", items));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Object>> unreadCount(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(Map.of("unreadCount", service.unreadCount(principal.memberId())));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Long id,
                                     @AuthenticationPrincipal MemberPrincipal principal) {
        service.markRead(id, principal.memberId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> readAll(@AuthenticationPrincipal MemberPrincipal principal) {
        service.markAllRead(principal.memberId());
        return ResponseEntity.ok().build();
    }
}
