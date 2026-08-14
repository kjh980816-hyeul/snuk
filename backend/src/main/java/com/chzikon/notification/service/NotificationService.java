package com.chzikon.notification.service;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.notification.domain.Notification;
import com.chzikon.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    /** 이벤트 발생 시 알림 적재 — 호출 트랜잭션에 합류(이벤트 실패 시 함께 롤백). */
    @Transactional
    public void notify(Long memberId, String type, String title, String body, String linkPath) {
        repository.save(new Notification(memberId, type, title, body, linkPath));
    }

    @Transactional(readOnly = true)
    public List<Notification> listMine(Long memberId) {
        return repository.findByMemberIdOrderByCreatedAtDesc(memberId, PageRequest.of(0, 30));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long memberId) {
        return repository.countByMemberIdAndReadAtIsNull(memberId);
    }

    @Transactional
    public void markRead(Long notificationId, Long memberId) {
        Notification n = repository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!n.getMemberId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        n.markRead();
    }

    @Transactional
    public void markAllRead(Long memberId) {
        repository.findByMemberIdAndReadAtIsNull(memberId).forEach(Notification::markRead);
    }
}
