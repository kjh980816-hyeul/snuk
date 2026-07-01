package com.chzikon.admin.service;

import com.chzikon.admin.domain.AdminLog;
import com.chzikon.admin.repository.AdminLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository repository;

    /** 어드민 행위 기록. detail 에는 절대 평문 키를 넣지 않는다(마스킹/요약만). */
    @Transactional
    public void record(Long actorMemberId, String action, String targetType, Long targetId, String detail) {
        repository.save(new AdminLog(actorMemberId, action, targetType, targetId, detail));
    }

    @Transactional(readOnly = true)
    public Page<AdminLog> findRecent(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
