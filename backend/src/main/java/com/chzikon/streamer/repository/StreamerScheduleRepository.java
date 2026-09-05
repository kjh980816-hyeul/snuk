package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface StreamerScheduleRepository extends JpaRepository<StreamerSchedule, Long> {

    List<StreamerSchedule> findByStreamerIdOrderByStartAtDesc(Long streamerId);
}
