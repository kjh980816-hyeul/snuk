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
    private final com.chzikon.streamer.repository.StreamerPostReportRepository reportRepository;

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

    /** 글 삭제 — 해당 스트리머(게시판 주인) + ADMIN 만(작성자 본인도 불가). 걸린 신고도 함께 정리. */
    @Transactional
    public void deletePost(Long postId, Long actorId) {
        StreamerPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Member actor = memberRepository.findById(actorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        boolean allowed = post.getStreamerId().equals(actorId)
                || actor.getRole() == Role.ADMIN;
        if (!allowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        reportRepository.deleteByPostId(postId);
        postRepository.delete(post);
    }

    /** 글 신고 — 로그인 회원 누구나(1인 1신고). 어드민이 신고함에서 확인 후 삭제/기각. (항목 3) */
    @Transactional
    public void reportPost(Long postId, Long reporterId, String reason) {
        StreamerPost post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (reportRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }
        try {
            reportRepository.save(new com.chzikon.streamer.domain.StreamerPostReport(
                    post.getId(), reporterId,
                    reason != null && reason.length() > 500 ? reason.substring(0, 500) : reason));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }
    }

    /** 어드민 신고함 — 최근 100건, 글 내용·작성자·신고자 닉네임 포함. */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReportsForAdmin() {
        var reports = reportRepository.findTop100ByOrderByCreatedAtDesc();
        var posts = postRepository.findAllById(
                        reports.stream().map(com.chzikon.streamer.domain.StreamerPostReport::getPostId).distinct().toList())
                .stream().collect(Collectors.toMap(StreamerPost::getId, Function.identity()));
        var memberIds = new java.util.HashSet<Long>();
        reports.forEach(r -> memberIds.add(r.getReporterId()));
        posts.values().forEach(p -> { memberIds.add(p.getAuthorId()); memberIds.add(p.getStreamerId()); });
        Map<Long, Member> members = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
        java.util.function.Function<Long, String> nick = (id) -> {
            Member m = members.get(id);
            return m != null ? m.getNickname() : ("회원#" + id);
        };
        return reports.stream().map(r -> {
            StreamerPost p = posts.get(r.getPostId());
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("reportId", r.getId());
            row.put("postId", r.getPostId());
            row.put("reason", r.getReason());
            row.put("reporterName", nick.apply(r.getReporterId()));
            row.put("createdAt", r.getCreatedAt());
            row.put("postTitle", p != null ? p.getTitle() : "(삭제된 글)");
            row.put("postContent", p != null ? p.getContent() : "");
            row.put("postAuthorName", p != null ? nick.apply(p.getAuthorId()) : "");
            row.put("streamerName", p != null ? nick.apply(p.getStreamerId()) : "");
            return row;
        }).toList();
    }

    /** 어드민 신고 기각(신고만 삭제, 글 유지). */
    @Transactional
    public void dismissReport(Long reportId) {
        reportRepository.deleteById(reportId);
    }

    /** 삭제 버튼 노출 — 게시판 주인 스트리머 + ADMIN 만(작성자 본인 제외). */
    private boolean canDelete(StreamerPost post, Member streamer, Long viewerId, boolean viewerIsAdmin) {
        if (viewerId == null) return false;
        return streamer.getId().equals(viewerId) || viewerIsAdmin;
    }
}
