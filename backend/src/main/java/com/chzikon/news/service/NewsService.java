package com.chzikon.news.service;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.member.service.MemberService;
import com.chzikon.news.dto.NewsDtos.NewsCreateRequest;
import com.chzikon.news.dto.NewsDtos.NewsResponse;
import com.chzikon.review.domain.Post;
import com.chzikon.review.domain.PostCategory;
import com.chzikon.review.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 스눅 뉴스 매거진 — 작성은 REPORTER+(백엔드 재검증), 조회는 공개. post 단일 테이블 분기(category=NEWS). */
@Service
@RequiredArgsConstructor
public class NewsService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    @Transactional(readOnly = true)
    public List<NewsResponse> list() {
        return withAuthors(postRepository.findByCategoryAndHiddenFalseOrderByCreatedAtDesc(PostCategory.NEWS));
    }

    @Transactional(readOnly = true)
    public NewsResponse get(Long id) {
        Post post = getVisibleNews(id);
        Member author = memberRepository.findById(post.getMemberId()).orElse(null);
        return NewsResponse.of(post, author);
    }

    @Transactional
    public NewsResponse write(Long memberId, NewsCreateRequest req) {
        Member member = requireReporter(memberId);
        Post post = new Post(PostCategory.NEWS, null, memberId, req.title(), req.content());
        post.changeThumbnail(req.thumbnailUrl());
        return NewsResponse.of(postRepository.save(post), member);
    }

    /** 수정 — 본인 기사 또는 ADMIN. */
    @Transactional
    public NewsResponse edit(Long id, Long memberId, NewsCreateRequest req) {
        Member actor = requireReporter(memberId);
        Post post = getVisibleNews(id);
        if (!post.isOwnedBy(memberId) && actor.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        post.edit(req.title(), req.content());
        post.changeThumbnail(req.thumbnailUrl());
        Member author = memberRepository.findById(post.getMemberId()).orElse(null);
        return NewsResponse.of(post, author);
    }

    /** 삭제 — 본인 기사 또는 ADMIN. */
    @Transactional
    public void delete(Long id, Long memberId) {
        Member actor = requireReporter(memberId);
        Post post = getVisibleNews(id);
        if (!post.isOwnedBy(memberId) && actor.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        postRepository.delete(post);
    }

    /** 업로드 등 부가 동작 전 권한 확인용(컨트롤러 공용). */
    @Transactional(readOnly = true)
    public void assertReporter(Long memberId) {
        requireReporter(memberId);
    }

    private Member requireReporter(Long memberId) {
        Member member = memberService.getById(memberId);
        if (!member.getRole().isReporterOrAbove()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_ROLE);
        }
        return member;
    }

    private Post getVisibleNews(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (post.getCategory() != PostCategory.NEWS || post.isHidden()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return post;
    }

    private List<NewsResponse> withAuthors(List<Post> posts) {
        Map<Long, Member> authors = memberRepository.findAllById(
                        posts.stream().map(Post::getMemberId).distinct().toList())
                .stream().collect(Collectors.toMap(Member::getId, Function.identity()));
        return posts.stream()
                .map(p -> NewsResponse.of(p, authors.get(p.getMemberId())))
                .toList();
    }
}
