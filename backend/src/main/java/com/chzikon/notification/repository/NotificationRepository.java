package com.chzikon.notification.repository;

import com.chzikon.notification.domain.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    List<Notification> findByMemberIdAndReadAtIsNull(Long memberId);
}
