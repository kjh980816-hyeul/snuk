package com.chzikon.community.service;

import com.chzikon.community.domain.*;
import com.chzikon.community.dto.CommunityDtos.*;
import com.chzikon.community.repository.*;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import com.chzikon.member.service.MemberService;
import com.chzikon.notice.domain.Notice;
import com.chzikon.notice.repository.NoticeRepository;
import com.chzikon.review.domain.Post;
import com.chzikon.review.domain.PostCategory;
import com.chzikon.review.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 스눅 커뮤니티.
 * 게시판 = 어드민이 코드 없이 관리(트리 2단 + 게시판마다 글쓰기/열람 최소 등급).
 * 시스템 게시판(source=NOTICE/NEWS)은 공지·뉴스 테이블을 그대로 창구만 커뮤니티로 쓴다
 * → 사이드바 공지사항 위젯 / 스눅 뉴스 페이지와 같은 데이터.
 * 수정·삭제 = 작성자 본인 + ADMIN(서비스에서 강제), 숨김 = ADMIN 전용.
 */
@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CommunityBoardRepository boardRepository;
    private final CommunityPostRepository postRepository;
    private final CommunityCommentRepository commentRepository;
    private final CommunityBuffRepository buffRepository;
    private final CommunityReportRepository reportRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;
    private final NoticeRepository noticeRepository;
    private final PostRepository newsRepository;

    // ================= 권한 =================

    private static int rank(Role role) {
        return switch (role) {
            case GUEST -> 0;
            case VIEWER -> 1;
            case STREAMER -> 2;
            case REPORTER -> 3;
            case ADMIN -> 4;
        };
    }

    private Role roleOf(Long memberId) {
        if (memberId == null) return Role.GUEST;
        return memberRepository.findById(memberId).map(Member::getRole).orElse(Role.GUEST);
    }

    private boolean canRead(CommunityBoard board, Role role) {
        return board.isVisible() && rank(role) >= rank(board.getReadRole());
    }

    private boolean canWrite(CommunityBoard board, Role role) {
        return board.isVisible() && rank(role) >= rank(board.getWriteRole());
    }

    // ================= 게시판 =================

    /** 공개 목록 — 노출 중 + 요청자가 볼 수 있는 게시판만(상위가 숨김/열람불가면 하위도 제외). */
    @Transactional(readOnly = true)
    public List<BoardResponse> boards(Long memberId) {
        Role role = roleOf(memberId);
        List<CommunityBoard> all = boardRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, CommunityBoard> byId = all.stream()
                .collect(Collectors.toMap(CommunityBoard::getId, Function.identity()));
        return all.stream()
                .filter(b -> canRead(b, role))
                .filter(b -> {
                    if (b.isGroup()) return true;
                    CommunityBoard parent = byId.get(b.getParentId());
                    return parent != null && canRead(parent, role);
                })
                .map(b -> BoardResponse.of(b, canWrite(b, role) && !hasChildren(b)))
                .toList();
    }

    /** 어드민 목록 — 숨김 포함 전체. */
    @Transactional(readOnly = true)
    public List<BoardResponse> allBoards() {
        return boardRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(b -> BoardResponse.of(b, true)).toList();
    }

    @Transactional
    public BoardResponse createBoard(BoardRequest req) {
        if (req.parentId() != null) {
            CommunityBoard parent = getBoard(req.parentId());
            if (!parent.isGroup()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
        }
        CommunityBoard board = new CommunityBoard(req.parentId(), req.name().trim(),
                req.sortOrder() != null ? req.sortOrder() : 0,
                req.visible() == null || req.visible(),
                req.writeRole(), req.readRole());
        return BoardResponse.of(boardRepository.save(board), true);
    }

    @Transactional
    public BoardResponse updateBoard(Long id, BoardRequest req) {
        CommunityBoard board = getBoard(id);
        board.update(req.name(), req.sortOrder(), req.visible(), req.writeRole(), req.readRole());
        return BoardResponse.of(board, true);
    }

    /** 삭제 — 글/하위 게시판이 있거나 시스템 게시판이면 거절(숨김으로 유도). */
    @Transactional
    public void deleteBoard(Long id) {
        CommunityBoard board = getBoard(id);
        if (board.isSystem()) {
            throw new BusinessException(ErrorCode.BOARD_NOT_AVAILABLE);
        }
        if (postRepository.existsByBoardId(id)) {
            throw new BusinessException(ErrorCode.BOARD_HAS_POSTS);
        }
        if (boardRepository.existsByParentId(id)) {
            throw new BusinessException(ErrorCode.BOARD_HAS_CHILDREN);
        }
        boardRepository.delete(board);
    }

    // ================= 글 목록 =================

    /**
     * 목록. boardId=null 이면 (열람 가능한) 전체.
     * sort: new(최신) / hot(조회) / buff(버프)
     */
    @Transactional(readOnly = true)
    public PostPage list(Long boardId, String sort, String q, int page, int size, Long memberId) {
        Role role = roleOf(memberId);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);

        if (boardId != null) {
            CommunityBoard board = getBoard(boardId);
            if (!canRead(board, role)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            if (board.isSystem()) {
                return systemList(board, q, safePage, safeSize);
            }
        }

        Collection<Long> scope = boardId != null ? boardScope(boardId) : readableBoardIds(role);
        if (scope.isEmpty()) {
            return new PostPage(List.of(), safePage, safeSize, 0, 0);
        }
        Pageable pageable = PageRequest.of(safePage, safeSize, sortOf(sort));
        Page<CommunityPost> found = postRepository.searchInBoards(scope, patternOf(q), pageable);
        return new PostPage(summaries(found.getContent()), found.getNumber(), found.getSize(),
                found.getTotalElements(), found.getTotalPages());
    }

    /** 인기글(사이드바) — 열람 가능한 범위 안에서. */
    @Transactional(readOnly = true)
    public List<PostSummary> popular(Long boardId, int limit, Long memberId) {
        Role role = roleOf(memberId);
        Collection<Long> scope = boardId != null ? boardScope(boardId) : readableBoardIds(role);
        if (scope.isEmpty()) return List.of();
        List<CommunityPost> posts = postRepository.findPopular(false, scope,
                PageRequest.of(0, Math.min(Math.max(limit, 1), 30)));
        return summaries(posts);
    }

    /** 상세 — 조회수 1 증가. 숨김 글은 작성자 본인/ADMIN 만 열람. */
    @Transactional
    public PostDetail get(Long id, Long memberId) {
        CommunityPost target = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        assertReadable(target.getBoardId(), memberId);
        if (target.isHidden() && !canManage(target, memberId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        postRepository.increaseViewCount(id);

        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return detailOf(post, memberId);
    }

    @Transactional(readOnly = true)
    public List<PostSummary> nearby(Long id) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        return summaries(postRepository
                .findTop5ByBoardIdAndHiddenFalseAndIdNotOrderByCreatedAtDesc(post.getBoardId(), id));
    }

    // ================= 글 작성/수정/삭제 =================

    @Transactional
    public PostDetail write(Long memberId, PostRequest req) {
        CommunityBoard board = getBoard(req.boardId());
        Role role = roleOf(memberId);
        if (!canWrite(board, role)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (board.isSystem()) {
            return writeSystem(board, memberId, req);
        }
        if (hasChildren(board)) {
            throw new BusinessException(ErrorCode.BOARD_NOT_AVAILABLE);
        }
        CommunityPost post = new CommunityPost(board.getId(), memberId, req.title().trim(), req.content());
        return detailOf(postRepository.save(post), memberId);
    }

    @Transactional
    public PostDetail edit(Long id, Long memberId, PostRequest req) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!canManage(post, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Long boardId = null;
        if (req.boardId() != null) {
            CommunityBoard target = getBoard(req.boardId());
            if (target.isSystem() || hasChildren(target) || !canWrite(target, roleOf(memberId))) {
                throw new BusinessException(ErrorCode.BOARD_NOT_AVAILABLE);
            }
            boardId = target.getId();
        }
        post.edit(boardId, req.title(), req.content());
        return detailOf(post, memberId);
    }

    @Transactional
    public void delete(Long id, Long memberId) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!canManage(post, memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        removePost(post);
    }

    /** 어드민 강제 삭제(권한은 컨트롤러 hasRole('ADMIN') + 여기서 재검증). */
    @Transactional
    public void deleteAsAdmin(Long id, Long adminId) {
        requireAdmin(adminId);
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        removePost(post);
    }

    /** 어드민 숨김/복구. */
    @Transactional
    public PostSummary setHidden(Long id, Long adminId, boolean hidden) {
        requireAdmin(adminId);
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        post.setHidden(hidden);
        return summaries(List.of(post)).get(0);
    }

    /** 내가 쓴 글 — 숨김 글도 본인에겐 보임. */
    @Transactional(readOnly = true)
    public List<PostSummary> myPosts(Long memberId) {
        return summaries(postRepository.findByMemberIdOrderByCreatedAtDesc(memberId));
    }

    /** 어드민 최근 글(관리용) — 숨김 포함. */
    @Transactional(readOnly = true)
    public List<PostSummary> recentForAdmin() {
        return summaries(postRepository.findTop200ByOrderByCreatedAtDesc());
    }

    // ================= 시스템 게시판(공지 · 뉴스) =================

    /** 공지 상세 — 커뮤니티 안에서 바로 읽는다(뉴스는 기사 페이지 /news/{id} 로 이동). */
    @Transactional(readOnly = true)
    public PostDetail noticeDetail(Long noticeId, Long memberId) {
        CommunityBoard board = systemBoard(BoardSource.NOTICE);
        if (!canRead(board, roleOf(memberId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Member author = memberRepository.findById(notice.getCreatedBy()).orElse(null);
        boolean mine = memberId != null && memberId.equals(notice.getCreatedBy());
        return PostDetail.ofSystem(notice.getId(), board, groupOf(board),
                notice.getTitle(), notice.getContent(), author, mine, notice.getCreatedAt());
    }

    /** 공지 삭제 — ADMIN(사이드바 공지사항에서도 함께 사라진다). */
    @Transactional
    public void deleteNotice(Long noticeId, Long memberId) {
        requireAdmin(memberId);
        noticeRepository.findById(noticeId).ifPresent(noticeRepository::delete);
    }

    private PostDetail writeSystem(CommunityBoard board, Long memberId, PostRequest req) {
        Member author = memberService.getById(memberId);
        String title = req.title().trim();
        if (board.getSource() == BoardSource.NOTICE) {
            Notice saved = noticeRepository.save(new Notice(title, req.content(), false, memberId));
            return PostDetail.ofSystem(saved.getId(), board, groupOf(board),
                    saved.getTitle(), saved.getContent(), author, true, saved.getCreatedAt());
        }
        Post saved = newsRepository.save(new Post(PostCategory.NEWS, null, memberId, title, req.content()));
        return PostDetail.ofSystem(saved.getId(), board, groupOf(board),
                saved.getTitle(), saved.getContent(), author, true, saved.getCreatedAt());
    }

    /** 시스템 게시판 목록 — 공지/뉴스를 커뮤니티 목록 행으로 맞춰서 페이징. */
    private PostPage systemList(CommunityBoard board, String q, int page, int size) {
        String needle = q == null ? null : q.trim().toLowerCase(Locale.ROOT);
        CommunityBoard group = groupOf(board);
        List<PostSummary> rows;

        if (board.getSource() == BoardSource.NOTICE) {
            List<Notice> notices = noticeRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            Map<Long, Member> authors = membersOf(notices.stream().map(Notice::getCreatedBy).toList());
            rows = notices.stream()
                    .filter(n -> matches(needle, n.getTitle(), n.getContent()))
                    .map(n -> PostSummary.ofSystem(n.getId(), board, group, n.getTitle(),
                            authors.get(n.getCreatedBy()), 0, n.getCreatedAt()))
                    .toList();
        } else {
            List<Post> news = newsRepository
                    .findByCategoryAndHiddenFalseOrderByCreatedAtDesc(PostCategory.NEWS);
            Map<Long, Member> authors = membersOf(news.stream().map(Post::getMemberId).toList());
            rows = news.stream()
                    .filter(n -> matches(needle, n.getTitle(), n.getContent()))
                    .map(n -> PostSummary.ofSystem(n.getId(), board, group, n.getTitle(),
                            authors.get(n.getMemberId()), 0, n.getCreatedAt()))
                    .toList();
        }

        int total = rows.size();
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        int totalPages = (int) Math.ceil((double) total / size);
        return new PostPage(rows.subList(from, to), page, size, total, totalPages);
    }

    private boolean matches(String needle, String title, String content) {
        if (needle == null || needle.isEmpty()) return true;
        String t = title == null ? "" : title.toLowerCase(Locale.ROOT);
        String c = content == null ? "" : content.toLowerCase(Locale.ROOT);
        return t.contains(needle) || c.contains(needle);
    }

    private CommunityBoard systemBoard(BoardSource source) {
        return boardRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(b -> b.getSource() == source)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    // ================= 버프(추천) =================

    @Transactional
    public BuffResponse toggleBuff(Long postId, Long memberId) {
        CommunityPost post = visiblePost(postId);
        assertReadable(post.getBoardId(), memberId);
        Optional<CommunityBuff> existing = buffRepository.findByPostIdAndMemberId(postId, memberId);
        if (existing.isPresent()) {
            buffRepository.delete(existing.get());
            post.addBuff(-1);
            return new BuffResponse(false, post.getBuffCount());
        }
        buffRepository.save(new CommunityBuff(postId, memberId));
        post.addBuff(1);
        return new BuffResponse(true, post.getBuffCount());
    }

    // ================= 댓글 =================

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long postId) {
        List<CommunityComment> comments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);
        Map<Long, Member> authors = membersOf(comments.stream().map(CommunityComment::getMemberId).toList());
        return comments.stream().map(c -> CommentResponse.of(c, authors.get(c.getMemberId()))).toList();
    }

    @Transactional
    public CommentResponse addComment(Long postId, Long memberId, CommentRequest req) {
        CommunityPost post = visiblePost(postId);
        assertReadable(post.getBoardId(), memberId);
        String content = req.content().trim();
        if (content.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Member member = memberService.getById(memberId);
        CommunityComment saved = commentRepository.save(new CommunityComment(postId, memberId, content));
        post.addComment(1);
        return CommentResponse.of(saved, member);
    }

    @Transactional
    public void deleteComment(Long postId, Long commentId, Long memberId) {
        CommunityComment comment = commentRepository.findByIdAndPostId(commentId, postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        Member actor = memberService.getById(memberId);
        if (!comment.isOwnedBy(memberId) && actor.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        commentRepository.delete(comment);
        postRepository.findById(postId).ifPresent(p -> p.addComment(-1));
    }

    // ================= 신고 =================

    @Transactional
    public void report(Long postId, Long reporterId, ReportRequest req) {
        visiblePost(postId);
        if (reportRepository.existsByPostIdAndReporterId(postId, reporterId)) {
            throw new BusinessException(ErrorCode.ALREADY_REPORTED);
        }
        String reason = req == null || req.reason() == null ? null : req.reason().trim();
        reportRepository.save(new CommunityReport(postId, reporterId, reason));
    }

    @Transactional(readOnly = true)
    public List<ReportView> reports() {
        List<CommunityReport> reports = reportRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, CommunityPost> posts = postRepository
                .findAllById(reports.stream().map(CommunityReport::getPostId).distinct().toList())
                .stream().collect(Collectors.toMap(CommunityPost::getId, Function.identity()));
        Map<Long, Member> reporters = membersOf(reports.stream().map(CommunityReport::getReporterId).toList());
        return reports.stream().map(r -> {
            CommunityPost post = posts.get(r.getPostId());
            Member reporter = reporters.get(r.getReporterId());
            return new ReportView(r.getId(), r.getPostId(),
                    post != null ? post.getTitle() : "삭제된 글",
                    r.getReporterId(), reporter != null ? reporter.getNickname() : "탈퇴 회원",
                    r.getReason(), post != null && post.isHidden(), r.getCreatedAt());
        }).toList();
    }

    @Transactional
    public void dismissReport(Long reportId) {
        reportRepository.findById(reportId).ifPresent(reportRepository::delete);
    }

    // ================= 내부 =================

    private void removePost(CommunityPost post) {
        Long id = post.getId();
        commentRepository.deleteByPostId(id);
        buffRepository.deleteByPostId(id);
        reportRepository.deleteByPostId(id);
        postRepository.delete(post);
    }

    private Sort sortOf(String sort) {
        String key = sort == null ? "new" : sort.toLowerCase(Locale.ROOT);
        return switch (key) {
            case "hot" -> Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "buff" -> Sort.by(Sort.Direction.DESC, "buffCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private String patternOf(String q) {
        if (q == null || q.isBlank()) return null;
        return "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
    }

    /** 게시판 범위 = 자신 + 하위 게시판. */
    private Collection<Long> boardScope(Long boardId) {
        List<Long> ids = new ArrayList<>();
        ids.add(boardId);
        boardRepository.findByParentId(boardId).forEach(b -> ids.add(b.getId()));
        return ids;
    }

    /** 요청자가 볼 수 있는 (시스템 아닌) 게시판 id — "전체 게시글" 범위. */
    private Collection<Long> readableBoardIds(Role role) {
        List<CommunityBoard> all = boardRepository.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, CommunityBoard> byId = all.stream()
                .collect(Collectors.toMap(CommunityBoard::getId, Function.identity()));
        return all.stream()
                .filter(b -> !b.isSystem())
                .filter(b -> canRead(b, role))
                .filter(b -> {
                    if (b.isGroup()) return true;
                    CommunityBoard parent = byId.get(b.getParentId());
                    return parent != null && canRead(parent, role);
                })
                .map(CommunityBoard::getId)
                .toList();
    }

    private void assertReadable(Long boardId, Long memberId) {
        CommunityBoard board = boardRepository.findById(boardId).orElse(null);
        if (board != null && !canRead(board, roleOf(memberId))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private boolean hasChildren(CommunityBoard board) {
        return board.isGroup() && boardRepository.existsByParentId(board.getId());
    }

    private CommunityBoard getBoard(Long id) {
        return boardRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    private CommunityBoard groupOf(CommunityBoard board) {
        return board.getParentId() == null ? null
                : boardRepository.findById(board.getParentId()).orElse(null);
    }

    private CommunityPost visiblePost(Long id) {
        CommunityPost post = postRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (post.isHidden()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return post;
    }

    private boolean canManage(CommunityPost post, Long memberId) {
        if (memberId == null) return false;
        if (post.isOwnedBy(memberId)) return true;
        return memberRepository.findById(memberId).map(m -> m.getRole() == Role.ADMIN).orElse(false);
    }

    private void requireAdmin(Long memberId) {
        Member actor = memberService.getById(memberId);
        if (actor.getRole() != Role.ADMIN) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private PostDetail detailOf(CommunityPost post, Long memberId) {
        CommunityBoard board = boardRepository.findById(post.getBoardId()).orElse(null);
        CommunityBoard group = board != null ? groupOf(board) : null;
        Member author = memberRepository.findById(post.getMemberId()).orElse(null);
        boolean buffed = memberId != null && buffRepository.existsByPostIdAndMemberId(post.getId(), memberId);
        boolean mine = memberId != null && post.isOwnedBy(memberId);
        return PostDetail.of(post, board, group, author, buffed, mine);
    }

    private List<PostSummary> summaries(List<CommunityPost> posts) {
        if (posts.isEmpty()) return List.of();
        Map<Long, CommunityBoard> boards = boardRepository.findAllByOrderBySortOrderAscIdAsc()
                .stream().collect(Collectors.toMap(CommunityBoard::getId, Function.identity()));
        Map<Long, Member> authors = membersOf(posts.stream().map(CommunityPost::getMemberId).toList());
        return posts.stream().map(p -> {
            CommunityBoard board = boards.get(p.getBoardId());
            CommunityBoard group = board != null && board.getParentId() != null
                    ? boards.get(board.getParentId()) : null;
            return PostSummary.of(p, board, group, authors.get(p.getMemberId()));
        }).toList();
    }

    private Map<Long, Member> membersOf(List<Long> memberIds) {
        List<Long> ids = memberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return memberRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Function.identity()));
    }
}
