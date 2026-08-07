package com.chzikon.community.repository;

import com.chzikon.community.domain.CommunityReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {

    List<CommunityReport> findAllByOrderByCreatedAtDesc();

    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);

    void deleteByPostId(Long postId);
}
