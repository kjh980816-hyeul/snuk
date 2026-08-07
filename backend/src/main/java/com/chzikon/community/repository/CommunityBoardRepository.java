package com.chzikon.community.repository;

import com.chzikon.community.domain.CommunityBoard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {

    List<CommunityBoard> findAllByOrderBySortOrderAscIdAsc();

    List<CommunityBoard> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);
}
