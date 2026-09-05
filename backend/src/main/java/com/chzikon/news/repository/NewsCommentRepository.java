package com.chzikon.news.repository;

import com.chzikon.news.domain.NewsComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NewsCommentRepository extends JpaRepository<NewsComment, Long> {

    List<NewsComment> findByPostIdOrderByCreatedAtAsc(Long postId);

    Optional<NewsComment> findByIdAndPostId(Long id, Long postId);
}
