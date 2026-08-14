package com.chzikon.streamer.controller;

import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.streamer.domain.*;
import com.chzikon.streamer.service.StreamerPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 스트리머 페이지 심화 + 방송도우미 도구 (V23).
 * GET=공개(SecurityConfig permitAll), 관리=본인/ADMIN(서비스 재검증).
 */
@RestController
@RequiredArgsConstructor
public class StreamerPageController {

    private final StreamerPageService service;

    // ---------- 공지 ----------

    @GetMapping("/api/streamers/{id}/notices")
    public ResponseEntity<List<Map<String, Object>>> notices(@PathVariable Long id) {
        return ResponseEntity.ok(service.listNotices(id).stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("body", n.getBody());
            m.put("important", n.isImportant());
            m.put("createdAt", n.getCreatedAt());
            return m;
        }).toList());
    }

    public record NoticeRequest(String title, String body, Boolean important) {
    }

    @PostMapping("/api/streamers/{id}/notices")
    public ResponseEntity<Map<String, Object>> createNotice(@PathVariable Long id,
                                                            @RequestBody NoticeRequest req,
                                                            @AuthenticationPrincipal MemberPrincipal principal) {
        StreamerNotice n = service.createNotice(id, principal.memberId(),
                req.title(), req.body(), Boolean.TRUE.equals(req.important()));
        return ResponseEntity.ok(Map.of("id", n.getId()));
    }

    @PutMapping("/api/streamer-notices/{noticeId}")
    public ResponseEntity<Void> updateNotice(@PathVariable Long noticeId,
                                             @RequestBody NoticeRequest req,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        service.updateNotice(noticeId, principal.memberId(), req.title(), req.body(), req.important());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/streamer-notices/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId,
                                             @AuthenticationPrincipal MemberPrincipal principal) {
        service.deleteNotice(noticeId, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ---------- 방송 일정 ----------

    @GetMapping("/api/streamers/{id}/schedules")
    public ResponseEntity<List<Map<String, Object>>> schedules(@PathVariable Long id) {
        return ResponseEntity.ok(service.listSchedules(id).stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("startAt", s.getStartAt());
            m.put("title", s.getTitle());
            m.put("game", s.getGame());
            m.put("mates", s.getMates());
            return m;
        }).toList());
    }

    public record ScheduleRequest(LocalDateTime startAt, String title, String game, String mates) {
    }

    @PostMapping("/api/streamers/{id}/schedules")
    public ResponseEntity<Map<String, Object>> createSchedule(@PathVariable Long id,
                                                              @RequestBody ScheduleRequest req,
                                                              @AuthenticationPrincipal MemberPrincipal principal) {
        StreamerSchedule s = service.createSchedule(id, principal.memberId(),
                req.startAt(), req.title(), req.game(), req.mates());
        return ResponseEntity.ok(Map.of("id", s.getId()));
    }

    @PutMapping("/api/streamer-schedules/{scheduleId}")
    public ResponseEntity<Void> updateSchedule(@PathVariable Long scheduleId,
                                               @RequestBody ScheduleRequest req,
                                               @AuthenticationPrincipal MemberPrincipal principal) {
        service.updateSchedule(scheduleId, principal.memberId(), req.startAt(), req.title(), req.game(), req.mates());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/streamer-schedules/{scheduleId}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId,
                                               @AuthenticationPrincipal MemberPrincipal principal) {
        service.deleteSchedule(scheduleId, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ---------- 위키 ----------

    @GetMapping("/api/streamers/{id}/wiki")
    public ResponseEntity<List<StreamerPageService.WikiSection>> wiki(@PathVariable Long id) {
        return ResponseEntity.ok(service.getWiki(id));
    }

    public record WikiRequest(List<StreamerPageService.WikiSection> sections) {
    }

    @PutMapping("/api/streamers/{id}/wiki")
    public ResponseEntity<List<StreamerPageService.WikiSection>> saveWiki(@PathVariable Long id,
                                                                          @RequestBody WikiRequest req,
                                                                          @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(service.saveWiki(id, principal.memberId(), req.sections()));
    }

    // ---------- 명령어 ----------

    @GetMapping("/api/streamers/{id}/commands")
    public ResponseEntity<List<Map<String, Object>>> commands(@PathVariable Long id) {
        return ResponseEntity.ok(service.listCommands(id).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("response", c.getResponse());
            m.put("enabled", c.isEnabled());
            m.put("sortOrder", c.getSortOrder());
            return m;
        }).toList());
    }

    public record CommandRequest(String name, String response, Boolean enabled, Integer sortOrder) {
    }

    @PostMapping("/api/streamers/{id}/commands")
    public ResponseEntity<Map<String, Object>> createCommand(@PathVariable Long id,
                                                             @RequestBody CommandRequest req,
                                                             @AuthenticationPrincipal MemberPrincipal principal) {
        StreamerCommand c = service.createCommand(id, principal.memberId(), req.name(), req.response(),
                req.enabled() == null || req.enabled(), req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.ok(Map.of("id", c.getId()));
    }

    @PutMapping("/api/streamer-commands/{commandId}")
    public ResponseEntity<Void> updateCommand(@PathVariable Long commandId,
                                              @RequestBody CommandRequest req,
                                              @AuthenticationPrincipal MemberPrincipal principal) {
        service.updateCommand(commandId, principal.memberId(), req.name(), req.response(), req.enabled(), req.sortOrder());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/streamer-commands/{commandId}")
    public ResponseEntity<Void> deleteCommand(@PathVariable Long commandId,
                                              @AuthenticationPrincipal MemberPrincipal principal) {
        service.deleteCommand(commandId, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ---------- 룰렛 ----------

    @GetMapping("/api/streamers/{id}/roulette")
    public ResponseEntity<List<Map<String, Object>>> roulette(@PathVariable Long id) {
        return ResponseEntity.ok(service.listRoulette(id).stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("label", r.getLabel());
            m.put("weight", r.getWeight());
            m.put("sortOrder", r.getSortOrder());
            return m;
        }).toList());
    }

    public record RouletteRequest(String label, Integer weight, Integer sortOrder) {
    }

    @PostMapping("/api/streamers/{id}/roulette")
    public ResponseEntity<Map<String, Object>> createRouletteItem(@PathVariable Long id,
                                                                  @RequestBody RouletteRequest req,
                                                                  @AuthenticationPrincipal MemberPrincipal principal) {
        StreamerRouletteItem item = service.createRouletteItem(id, principal.memberId(), req.label(),
                req.weight() != null ? req.weight() : 1, req.sortOrder() != null ? req.sortOrder() : 0);
        return ResponseEntity.ok(Map.of("id", item.getId()));
    }

    @PutMapping("/api/streamer-roulette/{itemId}")
    public ResponseEntity<Void> updateRouletteItem(@PathVariable Long itemId,
                                                   @RequestBody RouletteRequest req,
                                                   @AuthenticationPrincipal MemberPrincipal principal) {
        service.updateRouletteItem(itemId, principal.memberId(), req.label(), req.weight(), req.sortOrder());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/api/streamer-roulette/{itemId}")
    public ResponseEntity<Void> deleteRouletteItem(@PathVariable Long itemId,
                                                   @AuthenticationPrincipal MemberPrincipal principal) {
        service.deleteRouletteItem(itemId, principal.memberId());
        return ResponseEntity.noContent().build();
    }

    // ---------- 노래 신청 ----------

    @GetMapping("/api/streamers/{id}/songs")
    public ResponseEntity<Map<String, Object>> songs(@PathVariable Long id,
                                                     @AuthenticationPrincipal MemberPrincipal principal) {
        Long me = principal != null ? principal.memberId() : null;
        List<Map<String, Object>> queued = service.listQueuedSongs(id).stream()
                .map(s -> songRow(s, me)).toList();
        List<Map<String, Object>> recent = service.listRecentDecidedSongs(id).stream()
                .map(s -> songRow(s, me)).toList();
        return ResponseEntity.ok(Map.of("queued", queued, "recent", recent));
    }

    private Map<String, Object> songRow(StreamerSongRequest s, Long me) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("title", s.getTitle());
        m.put("status", s.getStatus().name());
        m.put("requesterName", service.memberOf(s.getRequesterId()).getNickname());
        m.put("mine", s.getRequesterId().equals(me));
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    public record SongRequestBody(String title) {
    }

    @PostMapping("/api/streamers/{id}/songs")
    public ResponseEntity<Map<String, Object>> requestSong(@PathVariable Long id,
                                                           @RequestBody SongRequestBody req,
                                                           @AuthenticationPrincipal MemberPrincipal principal) {
        StreamerSongRequest s = service.requestSong(id, principal.memberId(), req.title());
        return ResponseEntity.ok(Map.of("id", s.getId(), "status", s.getStatus().name()));
    }

    public record SongDecideBody(String status) {
    }

    /** 재생(PLAYED)/스킵(SKIPPED) 처리 — 해당 스트리머/ADMIN. */
    @PatchMapping("/api/streamer-songs/{songId}")
    public ResponseEntity<Void> decideSong(@PathVariable Long songId,
                                           @RequestBody SongDecideBody req,
                                           @AuthenticationPrincipal MemberPrincipal principal) {
        StreamerSongRequest.Status status;
        try {
            status = StreamerSongRequest.Status.valueOf(req.status());
        } catch (Exception e) {
            throw new com.chzikon.global.error.BusinessException(com.chzikon.global.error.ErrorCode.INVALID_INPUT);
        }
        service.decideSong(songId, principal.memberId(), status);
        return ResponseEntity.ok().build();
    }

    /** 신청 취소 — 신청자 본인/스트리머/ADMIN. */
    @DeleteMapping("/api/streamer-songs/{songId}")
    public ResponseEntity<Void> cancelSong(@PathVariable Long songId,
                                           @AuthenticationPrincipal MemberPrincipal principal) {
        service.cancelSong(songId, principal.memberId());
        return ResponseEntity.noContent().build();
    }
}
