package com.chzikon.community.repository;

import com.chzikon.community.domain.CommunityComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    List<CommunityComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    Optional<CommunityComment> findByIdAndPostId(Long id, Long postId);

    void deleteByPostId(Long postId);
}
