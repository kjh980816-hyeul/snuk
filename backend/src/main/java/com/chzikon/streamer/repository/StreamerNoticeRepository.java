package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerNotice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamerNoticeRepository extends JpaRepository<StreamerNotice, Long> {
    List<StreamerNotice> findByStreamerIdOrderByImportantDescCreatedAtDesc(Long streamerId);
}
