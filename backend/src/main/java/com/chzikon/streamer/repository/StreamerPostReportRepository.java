package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerPostReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamerPostReportRepository extends JpaRepository<StreamerPostReport, Long> {

    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);

    List<StreamerPostReport> findTop100ByOrderByCreatedAtDesc();

    void deleteByPostId(Long postId);
}
