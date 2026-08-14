package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.StreamerSongRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamerSongRequestRepository extends JpaRepository<StreamerSongRequest, Long> {
    List<StreamerSongRequest> findTop50ByStreamerIdAndStatusOrderByCreatedAtAsc(Long streamerId, StreamerSongRequest.Status status);

    List<StreamerSongRequest> findTop20ByStreamerIdAndStatusNotOrderByDecidedAtDesc(Long streamerId, StreamerSongRequest.Status status);

    long countByStreamerIdAndRequesterIdAndStatus(Long streamerId, Long requesterId, StreamerSongRequest.Status status);
}
