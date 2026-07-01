package com.chzikon.collab.repository;

import com.chzikon.collab.domain.ContentVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentVideoRepository extends JpaRepository<ContentVideo, Long> {
    List<ContentVideo> findAllByOrderBySortOrderAscIdAsc();

    List<ContentVideo> findByFeaturedTrueOrderBySortOrderAsc();
}
