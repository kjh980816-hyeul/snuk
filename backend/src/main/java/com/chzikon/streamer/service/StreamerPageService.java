package com.chzikon.streamer.service;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
import com.chzikon.notification.service.NotificationService;
import com.chzikon.streamer.domain.*;
import com.chzikon.streamer.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 스트리머 페이지 심화(공지/일정/위키) + 방송도우미 도구(명령어/룰렛/노래신청) — V23.
 * 조회=공개, 관리=본인(STREAMER+) 또는 ADMIN (프론트 가드 불신뢰, 전부 서버 재검증).
 */
@Service
@RequiredArgsConstructor
public class StreamerPageService {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int SONG_QUEUE_LIMIT_PER_REQUESTER = 3;

    private final StreamerNoticeRepository noticeRepository;
    private final StreamerScheduleRepository scheduleRepository;
    private final StreamerWikiRepository wikiRepository;
    private final StreamerCommandRepository commandRepository;
    private final StreamerRouletteItemRepository rouletteRepository;
    private final StreamerSongRequestRepository songRepository;
    private final MemberService memberService;
    private final NotificationService notificationService;

    /** 본인(STREAMER+) 또는 ADMIN 만 관리 가능. */
    private void requireOwner(Long streamerId, Long actorId) {
        Member actor = memberService.getById(actorId);
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (!streamerId.equals(actorId) || !actor.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    /** 페이지 대상 스트리머 검증(존재 + STREAMER+). */
    private void requireStreamer(Long streamerId) {
        Member target = memberService.getById(streamerId);
        if (!target.getRole().isStreamerOrAbove()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
    }

    // ---------- 공지 ----------

    @Transactional(readOnly = true)
    public List<StreamerNotice> listNotices(Long streamerId) {
        return noticeRepository.findByStreamerIdOrderByImportantDescCreatedAtDesc(streamerId);
    }

    @Transactional
    public StreamerNotice createNotice(Long streamerId, Long actorId, String title, String body, boolean important) {
        requireOwner(streamerId, actorId);
        requireStreamer(streamerId);
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "공지 제목을 입력해주세요.");
        }
        return noticeRepository.save(new StreamerNotice(streamerId, title.trim(), body, important));
    }

    @Transactional
    public StreamerNotice updateNotice(Long noticeId, Long actorId, String title, String body, Boolean important) {
        StreamerNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(notice.getStreamerId(), actorId);
        notice.update(title, body, important);
        return notice;
    }

    @Transactional
    public void deleteNotice(Long noticeId, Long actorId) {
        StreamerNotice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(notice.getStreamerId(), actorId);
        noticeRepository.delete(notice);
    }

    // ---------- 방송 일정 ----------

    /** 다가오는 일정(오늘 이후) + 지난 일정 일부는 프론트 필요 시 별도. */
    @Transactional(readOnly = true)
    public List<StreamerSchedule> listSchedules(Long streamerId) {
        return scheduleRepository.findByStreamerIdOrderByStartAtDesc(streamerId);
    }

    @Transactional
    public StreamerSchedule createSchedule(Long streamerId, Long actorId, LocalDateTime startAt,
                                           String title, String game, String mates) {
        requireOwner(streamerId, actorId);
        requireStreamer(streamerId);
        if (startAt == null || title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "일시와 제목을 입력해주세요.");
        }
        return scheduleRepository.save(new StreamerSchedule(streamerId, startAt, title.trim(), game, mates));
    }

    @Transactional
    public StreamerSchedule updateSchedule(Long scheduleId, Long actorId, LocalDateTime startAt,
                                           String title, String game, String mates) {
        StreamerSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(schedule.getStreamerId(), actorId);
        schedule.update(startAt, title, game, mates);
        return schedule;
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long actorId) {
        StreamerSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(schedule.getStreamerId(), actorId);
        scheduleRepository.delete(schedule);
    }

    // ---------- 위키 ----------

    public record WikiSection(String t, String b) {
    }

    @Transactional(readOnly = true)
    public List<WikiSection> getWiki(Long streamerId) {
        return wikiRepository.findById(streamerId)
                .map(w -> parseSections(w.getSections()))
                .orElse(List.of());
    }

    @Transactional
    public List<WikiSection> saveWiki(Long streamerId, Long actorId, List<WikiSection> sections) {
        requireOwner(streamerId, actorId);
        requireStreamer(streamerId);
        List<WikiSection> clean = sections == null ? List.of()
                : sections.stream()
                .filter(s -> s != null && s.t() != null && !s.t().isBlank())
                .map(s -> new WikiSection(s.t().trim(), s.b() != null ? s.b() : ""))
                .toList();
        String json = toJson(clean);
        wikiRepository.findById(streamerId).ifPresentOrElse(
                w -> w.update(json),
                () -> wikiRepository.save(new StreamerWiki(streamerId, json)));
        return clean;
    }

    private List<WikiSection> parseSections(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readerForListOf(WikiSection.class).readValue(json);
        } catch (Exception e) {
            return List.of();
        }
    }

    private String toJson(List<WikiSection> sections) {
        try {
            return MAPPER.writeValueAsString(sections);
        } catch (Exception e) {
            return "[]";
        }
    }

    // ---------- 명령어 ----------

    @Transactional(readOnly = true)
    public List<StreamerCommand> listCommands(Long streamerId) {
        return commandRepository.findByStreamerIdOrderBySortOrderAscIdAsc(streamerId);
    }

    @Transactional
    public StreamerCommand createCommand(Long streamerId, Long actorId, String name, String response,
                                         boolean enabled, int sortOrder) {
        requireOwner(streamerId, actorId);
        requireStreamer(streamerId);
        if (name == null || name.isBlank() || response == null || response.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "명령어와 응답을 입력해주세요.");
        }
        String cleanName = name.trim().startsWith("!") ? name.trim() : "!" + name.trim();
        if (commandRepository.existsByStreamerIdAndName(streamerId, cleanName)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 있는 명령어입니다: " + cleanName);
        }
        try {
            return commandRepository.save(new StreamerCommand(streamerId, cleanName, response.trim(), enabled, sortOrder));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 있는 명령어입니다: " + cleanName);
        }
    }

    @Transactional
    public StreamerCommand updateCommand(Long commandId, Long actorId, String name, String response,
                                         Boolean enabled, Integer sortOrder) {
        StreamerCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(command.getStreamerId(), actorId);
        String cleanName = name == null ? null : (name.trim().startsWith("!") ? name.trim() : "!" + name.trim());
        command.update(cleanName, response, enabled, sortOrder);
        return command;
    }

    @Transactional
    public void deleteCommand(Long commandId, Long actorId) {
        StreamerCommand command = commandRepository.findById(commandId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(command.getStreamerId(), actorId);
        commandRepository.delete(command);
    }

    // ---------- 룰렛 ----------

    @Transactional(readOnly = true)
    public List<StreamerRouletteItem> listRoulette(Long streamerId) {
        return rouletteRepository.findByStreamerIdOrderBySortOrderAscIdAsc(streamerId);
    }

    @Transactional
    public StreamerRouletteItem createRouletteItem(Long streamerId, Long actorId, String label, int weight, int sortOrder) {
        requireOwner(streamerId, actorId);
        requireStreamer(streamerId);
        if (label == null || label.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "항목 이름을 입력해주세요.");
        }
        return rouletteRepository.save(new StreamerRouletteItem(streamerId, label.trim(), weight, sortOrder));
    }

    @Transactional
    public StreamerRouletteItem updateRouletteItem(Long itemId, Long actorId, String label, Integer weight, Integer sortOrder) {
        StreamerRouletteItem item = rouletteRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(item.getStreamerId(), actorId);
        item.update(label, weight, sortOrder);
        return item;
    }

    @Transactional
    public void deleteRouletteItem(Long itemId, Long actorId) {
        StreamerRouletteItem item = rouletteRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(item.getStreamerId(), actorId);
        rouletteRepository.delete(item);
    }

    // ---------- 노래 신청 ----------

    @Transactional(readOnly = true)
    public List<StreamerSongRequest> listQueuedSongs(Long streamerId) {
        return songRepository.findTop50ByStreamerIdAndStatusOrderByCreatedAtAsc(streamerId, StreamerSongRequest.Status.QUEUED);
    }

    @Transactional(readOnly = true)
    public List<StreamerSongRequest> listRecentDecidedSongs(Long streamerId) {
        return songRepository.findTop20ByStreamerIdAndStatusNotOrderByDecidedAtDesc(streamerId, StreamerSongRequest.Status.QUEUED);
    }

    /** 신청 — 로그인 회원 누구나, 스트리머당 대기 3곡 제한. */
    @Transactional
    public StreamerSongRequest requestSong(Long streamerId, Long requesterId, String title) {
        requireStreamer(streamerId);
        if (title == null || title.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "곡 제목을 입력해주세요.");
        }
        if (songRepository.countByStreamerIdAndRequesterIdAndStatus(
                streamerId, requesterId, StreamerSongRequest.Status.QUEUED) >= SONG_QUEUE_LIMIT_PER_REQUESTER) {
            throw new BusinessException(ErrorCode.CONFLICT,
                    "대기열에 이미 " + SONG_QUEUE_LIMIT_PER_REQUESTER + "곡을 신청했어요. 재생 후 다시 신청해주세요.");
        }
        StreamerSongRequest saved = songRepository.save(new StreamerSongRequest(streamerId, requesterId, title.trim()));
        Member requester = memberService.getById(requesterId);
        notificationService.notify(streamerId, "SONG_REQUESTED",
                "노래 신청이 들어왔어요",
                requester.getNickname() + " — " + saved.getTitle(),
                "/streamers/" + streamerId);
        return saved;
    }

    /** 재생/스킵 처리 — 해당 스트리머 본인 또는 ADMIN. */
    @Transactional
    public StreamerSongRequest decideSong(Long songId, Long actorId, StreamerSongRequest.Status status) {
        if (status == StreamerSongRequest.Status.QUEUED) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        StreamerSongRequest song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        requireOwner(song.getStreamerId(), actorId);
        if (!song.isQueued()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청곡입니다.");
        }
        song.decide(status);
        return song;
    }

    /** 신청 취소 — 신청자 본인(대기 중일 때만) 또는 스트리머/ADMIN. */
    @Transactional
    public void cancelSong(Long songId, Long actorId) {
        StreamerSongRequest song = songRepository.findById(songId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!song.getRequesterId().equals(actorId)) {
            requireOwner(song.getStreamerId(), actorId);
        }
        if (!song.isQueued()) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 처리된 신청곡입니다.");
        }
        songRepository.delete(song);
    }

    /** 신청자 닉네임 조회용(컨트롤러 응답 조립). */
    @Transactional(readOnly = true)
    public Member memberOf(Long memberId) {
        return memberService.getById(memberId);
    }
}
