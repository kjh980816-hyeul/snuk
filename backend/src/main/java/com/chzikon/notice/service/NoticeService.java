package com.chzikon.notice.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.notice.domain.Notice;
import com.chzikon.notice.dto.NoticeDtos.NoticeRequest;
import com.chzikon.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final int MAX_LIST = 20;

    private final NoticeRepository noticeRepository;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public List<Notice> findLatest(int limit) {
        int size = Math.max(1, Math.min(limit, MAX_LIST));
        return noticeRepository.findLatest(PageRequest.of(0, size));
    }

    @Transactional(readOnly = true)
    public Notice getById(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional
    public Notice create(NoticeRequest request, Long actorId) {
        Notice notice = noticeRepository.save(
                new Notice(request.title(), request.content(), request.pinned(), actorId));
        adminLogService.record(actorId, "NOTICE_CREATE", "notice", notice.getId(), request.title());
        return notice;
    }

    @Transactional
    public Notice update(Long id, NoticeRequest request, Long actorId) {
        Notice notice = getById(id);
        notice.update(request.title(), request.content(), request.pinned());
        adminLogService.record(actorId, "NOTICE_UPDATE", "notice", id, request.title());
        return notice;
    }

    @Transactional
    public void delete(Long id, Long actorId) {
        Notice notice = getById(id);
        noticeRepository.delete(notice);
        adminLogService.record(actorId, "NOTICE_DELETE", "notice", id, notice.getTitle());
    }
}
