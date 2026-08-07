package com.chzikon.community.repository;

import com.chzikon.community.domain.CommunityBuff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityBuffRepository extends JpaRepository<CommunityBuff, Long> {

    Optional<CommunityBuff> findByPostIdAndMemberId(Long postId, Long memberId);

    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    void deleteByPostId(Long postId);
}
