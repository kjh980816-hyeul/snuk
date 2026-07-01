package com.chzikon.collab.repository;

import com.chzikon.collab.domain.CollabGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollabGameRepository extends JpaRepository<CollabGame, Long> {
    List<CollabGame> findAllByOrderBySortOrderAscIdAsc();
}
