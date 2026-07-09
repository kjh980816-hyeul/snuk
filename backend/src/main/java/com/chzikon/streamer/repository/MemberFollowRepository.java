package com.chzikon.streamer.repository;

import com.chzikon.streamer.domain.MemberFollow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberFollowRepository extends JpaRepository<MemberFollow, Long> {

    boolean existsByFollowerIdAndStreamerId(Long followerId, Long streamerId);

    Optional<MemberFollow> findByFollowerIdAndStreamerId(Long followerId, Long streamerId);

    long countByStreamerId(Long streamerId);
}
