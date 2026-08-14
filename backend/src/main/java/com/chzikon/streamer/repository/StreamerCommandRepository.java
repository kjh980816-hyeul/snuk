package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamerCommandRepository extends JpaRepository<StreamerCommand, Long> {
    List<StreamerCommand> findByStreamerIdOrderBySortOrderAscIdAsc(Long streamerId);

    boolean existsByStreamerIdAndName(Long streamerId, String name);
}
