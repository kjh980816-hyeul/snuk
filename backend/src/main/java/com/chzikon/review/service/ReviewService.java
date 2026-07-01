package com.chzikon.review.service;

import com.chzikon.admin.service.AdminLogService;
import com.chzikon.campaign.domain.CampaignApplication;
import com.chzikon.campaign.repository.CampaignApplicationRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.review.domain.Post;
import com.chzikon.review.domain.PostCategory;
import com.chzikon.review.dto.ReviewCreateRequest;
import com.chzikon.review.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final PostRepository postRepository;
    private final CampaignApplicationRepository applicationRepository;
    private final AdminLogService adminLogService;

    /** 후기 작성: 배정받은 참가자(APPROVED)만(REV-01, 백엔드 검증). */
    @Transactional
    public Post write(Long campaignId, Long memberId, ReviewCreateRequest req) {
        boolean isParticipant = applicationRepository.findByCampaignIdAndMemberId(campaignId, memberId)
                .map(CampaignApplication::isApproved)
                .orElse(false);
        if (!isParticipant) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_PARTICIPANT);
        }
        Post post = new Post(PostCategory.REVIEW, campaignId, memberId, req.title(), req.content());
        return postRepository.save(post);
    }

    @Transactional
    public Post edit(Long reviewId, Long memberId, ReviewCreateRequest req) {
        Post post = postRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!post.isOwnedBy(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        post.edit(req.title(), req.content());
        return post;
    }

    @Transactional(readOnly = true)
    public List<Post> listByCampaign(Long campaignId) {
        return postRepository.findByCategoryAndCampaignIdAndHiddenFalseOrderByCreatedAtDesc(
                PostCategory.REVIEW, campaignId);
    }

    @Transactional(readOnly = true)
    public List<Post> listAll() {
        return postRepository.findByCategoryAndHiddenFalseOrderByCreatedAtDesc(PostCategory.REVIEW);
    }

    /** 대표 노출/숨김(REV-04). */
    @Transactional
    public void setHidden(Long reviewId, boolean hidden, Long actorId) {
        Post post = postRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        post.setHidden(hidden);
        adminLogService.record(actorId, "REVIEW_HIDE", "post", reviewId, "hidden=" + hidden);
    }
}
