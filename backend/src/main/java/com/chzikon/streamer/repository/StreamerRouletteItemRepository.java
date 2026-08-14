package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerRouletteItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamerRouletteItemRepository extends JpaRepository<StreamerRouletteItem, Long> {
    List<StreamerRouletteItem> findByStreamerIdOrderBySortOrderAscIdAsc(Long streamerId);
}
