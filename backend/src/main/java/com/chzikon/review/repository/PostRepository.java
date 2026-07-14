package com.chzikon.review.repository;

import com.chzikon.review.domain.Post;
import com.chzikon.review.domain.PostCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByCategoryAndCampaignIdAndHiddenFalseOrderByCreatedAtDesc(
            PostCategory category, Long campaignId);

    List<Post> findByCategoryAndHiddenFalseOrderByCreatedAtDesc(PostCategory category);

    List<Post> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    /** 후기 작성 여부(마감 경고/연장 판단). */
    boolean existsByCategoryAndCampaignIdAndMemberId(PostCategory category, Long campaignId, Long memberId);
}
