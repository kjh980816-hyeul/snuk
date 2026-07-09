package com.chzikon.streamer.service;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.dto.StreamerPublicView;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.streamer.domain.MemberFollow;
import com.chzikon.streamer.domain.StreamerPost;
import com.chzikon.streamer.dto.StreamerDtos.StreamerPostRequest;
import com.chzikon.streamer.dto.StreamerDtos.StreamerPostResponse;
import com.chzikon.streamer.dto.StreamerDtos.StreamerProfileResponse;
import com.chzikon.streamer.repository.MemberFollowRepository;
import com.chzikon.streamer.repository.StreamerPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StreamerProfileService {

    private final MemberRepository memberRepository;
    private final MemberFollowRepository followRepository;
    private final StreamerPostRepository postRepository;

    /** 스트리머(STREAMER/ADMIN 등급) 조회 — 그 외 회원은 프로필 비공개. */
    private Member getStreamer(Long streamerId) {
        Member member = memberRepository.findById(streamerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (member.getRole() != Role.STREAMER && member.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return member;
    }

    @Transactional(readOnly = true)
    public StreamerProfileResponse getProfile(Long streamerId, Long viewerId) {
        Member streamer = getStreamer(streamerId);
        long followCount = followRepository.countByStreamerId(streamerId);
        boolean following = viewerId != null
                && followRepository.existsByFollowerIdAndStreamerId(viewerId, streamerId);
        return new StreamerProfileResponse(StreamerPublicView.from(streamer), followCount, following);
    }

    /** 팔로우 — 본인 팔로우 금지, 중복은 멱등 처리. */
    @Transactional
    public long follow(Long streamerId, Long followerId) {
        getStreamer(streamerId);
        if (streamerId.equals(followerId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "자기 자신은 팔로우할 수 없습니다.");
        }
        if (!followRepository.existsByFollowerIdAndStreamerId(followerId, streamerId)) {
            try {
                followRepository.save(new MemberFollow(followerId, streamerId));
            } catch (DataIntegrityViolationException ignored) {
                // 동시 중복 팔로우 — 멱등
            }
        }
        return followRepository.countByStreamerId(streamerId);
    }

    @Transactional
    public long unfollow(Long streamerId, Long followerId) {
        followRepository.findByFollowerIdAndStreamerId(followerId, streamerId)
                .ifPresent(followRepository::delete);
        return followRepository.countByStreamerId(streamerId);
    }

    // ---------- 개인 게시판 ----------

    @Transactional(readOnly = true)
    public List<StreamerPostResponse> listPosts(Long streamerId, Long viewerId) {
        Member streamer = getStreamer(streamerId);
        List<StreamerPost> posts = postRepository.findTop100ByStreamerIdOrderByCreatedAtDesc(streamerId);
        Map<Long, Member> authors = memberRepository.findAllById(
                        posts.stream().map(StreamerPost::getAuthorId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        boolean viewerIsAdmin = viewerId != null && memberRepository.findById(viewerId)
                .map(m -> m.getRole() == Role.ADMIN).orElse(false);
        return posts.stream().map(p -> StreamerPostResponse.of(
                p, authors.get(p.getAuthorId()),
                canDelete(p, streamer, viewerId, viewerIsAdmin))).toList();
    }

    /** 글 작성 — 로그인 회원 누구나. */
    @Transactional
    public StreamerPostResponse writePost(Long streamerId, Long authorId, StreamerPostRequest request) {
        Member streamer = getStreamer(streamerId);
        Member author = memberRepository.findById(authorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        StreamerPost saved = postRepository.save(
                new StreamerPost(streamerId, authorId, request.title(), request.content()));
        return StreamerPostResponse.of(saved, author,
                canDelete(saved, streamer, authorId, author.getRole() == Role.ADMIN));
    }

    /** 글 삭제 — 작성자 본인 + 해당 스트리머 + ADMIN 만. */
    @Transactional
    public void deletePost(Long postId, Long actorId) {
        StreamerPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Member actor = memberRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        boolean allowed = post.getAuthorId().equals(actorId)
                || post.getStreamerId().equals(actorId)
                || actor.getRole() == Role.ADMIN;
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        postRepository.delete(post);
    }

    private boolean canDelete(StreamerPost post, Member streamer, Long viewerId, boolean viewerIsAdmin) {
        if (viewerId == null) return false;
        return post.getAuthorId().equals(viewerId) || streamer.getId().equals(viewerId) || viewerIsAdmin;
    }
}
