package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerPost;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamerPostRepository extends JpaRepository<StreamerPost, Long> {

    List<StreamerPost> findTop100ByStreamerIdOrderByCreatedAtDesc(Long streamerId);
}
