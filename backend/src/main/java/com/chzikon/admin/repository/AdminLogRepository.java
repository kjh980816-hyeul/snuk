package com.chzikon.admin.repository;

import com.chzikon.admin.domain.AdminLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminLogRepository extends JpaRepository<AdminLog, Long> {

    Page<AdminLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
