package com.chzikon.community.service;

import com.chzikon.community.dto.CommunityDtos.*;
import com.chzikon.community.domain.BoardSource;
import com.chzikon.community.repository.CommunityBoardRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Provider;
import com.chzikon.member.domain.Role;
import com.chzikon.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 커뮤니티 핵심 규칙:
 * 시드 게시판 로드 / 글쓰기·수정·삭제 권한(작성자+ADMIN) / 버프 토글 / 댓글 카운트 /
 * 숨김 글 비노출 / 1인 1신고 / 글 있는 게시판 삭제 거부.
 */
@SpringBootTest
class CommunityServiceTest {

    @Autowired CommunityService communityService;
    @Autowired CommunityBoardRepository boardRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired com.chzikon.notice.repository.NoticeRepository noticeRepository;
    @Autowired com.chzikon.review.repository.PostRepository newsRepository;

    private Member member(String channelId, String nick, Role role) {
        return memberRepository.save(Member.create(Provider.CHZZK, channelId, nick, null, 10, role));
    }

    /** V18 시드로 들어온 하위 게시판 하나(글 작성 가능). */
    private Long leafBoardId() {
        return boardRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(b -> b.getParentId() != null && b.getSource() == null)
                .findFirst().orElseThrow().getId();
    }

    @Test
    void seed_boards_are_loaded_as_tree() {
        List<BoardResponse> boards = communityService.boards(null);
        assertThat(boards).isNotEmpty();
        assertThat(boards).anyMatch(b -> b.parentId() == null);
        assertThat(boards).anyMatch(b -> b.parentId() != null);
    }

    @Test
    void write_then_list_and_detail() {
        Member writer = member("cm-w1", "글쓴이", Role.VIEWER);
        Long boardId = leafBoardId();

        PostDetail written = communityService.write(writer.getId(),
                new PostRequest(boardId, "테스트 글 제목", "본문입니다"));
        assertThat(written.id()).isNotNull();
        assertThat(written.mine()).isTrue();

        PostPage page = communityService.list(boardId, "new", null, 0, 20, null);
        assertThat(page.items()).anyMatch(p -> p.id().equals(written.id()));

        // 조회 시 조회수 증가
        PostDetail read = communityService.get(written.id(), null);
        assertThat(read.viewCount()).isEqualTo(1);
        assertThat(read.mine()).isFalse();

        // 검색
        assertThat(communityService.list(null, "new", "테스트 글", 0, 20, null).items())
                .anyMatch(p -> p.id().equals(written.id()));
    }

    @Test
    void only_author_or_admin_can_edit_and_delete() {
        Member writer = member("cm-w2", "작성자", Role.VIEWER);
        Member stranger = member("cm-s2", "남", Role.VIEWER);
        Member admin = member("cm-a2", "관리자", Role.ADMIN);
        Long boardId = leafBoardId();

        PostDetail post = communityService.write(writer.getId(), new PostRequest(boardId, "권한 확인", "본문"));

        assertThatThrownBy(() -> communityService.edit(post.id(), stranger.getId(),
                new PostRequest(boardId, "몰래 수정", "본문")))
                .isInstanceOf(BusinessException.class);

        PostDetail edited = communityService.edit(post.id(), writer.getId(),
                new PostRequest(boardId, "본인 수정", "수정 본문"));
        assertThat(edited.title()).isEqualTo("본인 수정");

        // ADMIN 은 남의 글도 수정/삭제 가능
        communityService.edit(post.id(), admin.getId(), new PostRequest(boardId, "관리자 수정", "본문"));
        communityService.delete(post.id(), admin.getId());
        assertThatThrownBy(() -> communityService.get(post.id(), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void buff_toggles_and_comments_are_counted() {
        Member writer = member("cm-w3", "작성자3", Role.VIEWER);
        Member reader = member("cm-r3", "독자3", Role.VIEWER);
        Long boardId = leafBoardId();
        PostDetail post = communityService.write(writer.getId(), new PostRequest(boardId, "버프 확인", "본문"));

        BuffResponse on = communityService.toggleBuff(post.id(), reader.getId());
        assertThat(on.buffed()).isTrue();
        assertThat(on.buffCount()).isEqualTo(1);
        assertThat(communityService.get(post.id(), reader.getId()).buffed()).isTrue();

        BuffResponse off = communityService.toggleBuff(post.id(), reader.getId());
        assertThat(off.buffed()).isFalse();
        assertThat(off.buffCount()).isZero();

        communityService.addComment(post.id(), reader.getId(), new CommentRequest("댓글입니다"));
        assertThat(communityService.comments(post.id())).hasSize(1);
        assertThat(communityService.get(post.id(), null).commentCount()).isEqualTo(1);

        Long commentId = communityService.comments(post.id()).get(0).id();
        communityService.deleteComment(post.id(), commentId, reader.getId());
        assertThat(communityService.comments(post.id())).isEmpty();
        assertThat(communityService.get(post.id(), null).commentCount()).isZero();
    }

    @Test
    void hidden_post_disappears_from_list_but_owner_and_admin_can_read() {
        Member writer = member("cm-w4", "작성자4", Role.VIEWER);
        Member admin = member("cm-a4", "관리자4", Role.ADMIN);
        Member other = member("cm-o4", "타인4", Role.VIEWER);
        Long boardId = leafBoardId();
        PostDetail post = communityService.write(writer.getId(), new PostRequest(boardId, "숨김 확인", "본문"));

        communityService.setHidden(post.id(), admin.getId(), true);

        assertThat(communityService.list(boardId, "new", "숨김 확인", 0, 20, null).items()).isEmpty();
        assertThatThrownBy(() -> communityService.get(post.id(), other.getId()))
                .isInstanceOf(BusinessException.class);
        assertThat(communityService.get(post.id(), writer.getId()).hidden()).isTrue();
        assertThat(communityService.myPosts(writer.getId())).anyMatch(p -> p.id().equals(post.id()));

        // 일반 회원은 숨김 처리 불가
        assertThatThrownBy(() -> communityService.setHidden(post.id(), other.getId(), false))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void report_is_once_per_member() {
        Member writer = member("cm-w5", "작성자5", Role.VIEWER);
        Member reporter = member("cm-r5", "신고자5", Role.VIEWER);
        Long boardId = leafBoardId();
        PostDetail post = communityService.write(writer.getId(), new PostRequest(boardId, "신고 확인", "본문"));

        communityService.report(post.id(), reporter.getId(), new ReportRequest("스팸입니다"));
        assertThatThrownBy(() -> communityService.report(post.id(), reporter.getId(), new ReportRequest("또 신고")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.ALREADY_REPORTED.getMessage());

        assertThat(communityService.reports()).anyMatch(r -> r.postId().equals(post.id()));
    }

    @Test
    void board_with_posts_cannot_be_deleted_and_group_with_children_is_not_writable() {
        Member writer = member("cm-w6", "작성자6", Role.VIEWER);
        Long boardId = leafBoardId();
        communityService.write(writer.getId(), new PostRequest(boardId, "게시판 삭제 확인", "본문"));

        assertThatThrownBy(() -> communityService.deleteBoard(boardId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.BOARD_HAS_POSTS.getMessage());

        Long groupWithChildren = boardRepository.findById(boardId).orElseThrow().getParentId();
        assertThatThrownBy(() -> communityService.write(writer.getId(),
                new PostRequest(groupWithChildren, "그룹에 직접 쓰기", "본문")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.BOARD_NOT_AVAILABLE.getMessage());
    }

    @Test
    void admin_can_create_update_and_delete_board() {
        BoardResponse created = communityService.createBoard(new BoardRequest(null, "테스트그룹", 99, true, null, null));
        assertThat(communityService.boards(null)).anyMatch(b -> b.id().equals(created.id()));

        communityService.updateBoard(created.id(), new BoardRequest(null, "테스트그룹2", 98, false, null, null));
        assertThat(communityService.boards(null)).noneMatch(b -> b.id().equals(created.id()));
        assertThat(communityService.allBoards()).anyMatch(b -> b.id().equals(created.id()));

        communityService.deleteBoard(created.id());
        assertThat(communityService.allBoards()).noneMatch(b -> b.id().equals(created.id()));
    }

    @Test
    void board_write_role_is_enforced() {
        Member viewer = member("cm-w7", "일반회원", Role.VIEWER);
        Member streamer = member("cm-s7", "스트리머", Role.STREAMER);
        BoardResponse board = communityService.createBoard(
                new BoardRequest(null, "스트리머 전용", 90, true, Role.STREAMER, Role.GUEST));

        assertThatThrownBy(() -> communityService.write(viewer.getId(),
                new PostRequest(board.id(), "일반회원이 쓰기", "본문")))
                .isInstanceOf(BusinessException.class);

        PostDetail ok = communityService.write(streamer.getId(),
                new PostRequest(board.id(), "스트리머가 쓰기", "본문"));
        assertThat(ok.id()).isNotNull();

        // 목록에서도 canWrite 로 구분된다
        assertThat(communityService.boards(viewer.getId()))
                .filteredOn(b -> b.id().equals(board.id())).allMatch(b -> !b.canWrite());
        assertThat(communityService.boards(streamer.getId()))
                .filteredOn(b -> b.id().equals(board.id())).allMatch(BoardResponse::canWrite);
    }

    @Test
    void board_read_role_hides_board_and_posts_from_lower_roles() {
        Member viewer = member("cm-w8", "일반회원8", Role.VIEWER);
        Member admin = member("cm-a8", "관리자8", Role.ADMIN);
        BoardResponse secret = communityService.createBoard(
                new BoardRequest(null, "운영 전용", 91, true, Role.ADMIN, Role.ADMIN));
        PostDetail post = communityService.write(admin.getId(),
                new PostRequest(secret.id(), "내부 공유", "본문"));

        assertThat(communityService.boards(viewer.getId())).noneMatch(b -> b.id().equals(secret.id()));
        assertThat(communityService.boards(admin.getId())).anyMatch(b -> b.id().equals(secret.id()));
        // 전체 게시글에도 안 섞인다
        assertThat(communityService.list(null, "new", "내부 공유", 0, 20, viewer.getId()).items()).isEmpty();
        assertThat(communityService.list(null, "new", "내부 공유", 0, 20, admin.getId()).items()).isNotEmpty();
        assertThatThrownBy(() -> communityService.get(post.id(), viewer.getId()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void system_boards_bridge_notice_and_news() {
        Member admin = member("cm-a9", "관리자9", Role.ADMIN);
        Member viewer = member("cm-v9", "일반회원9", Role.VIEWER);
        Long noticeBoard = systemBoardId(BoardSource.NOTICE);
        Long newsBoard = systemBoardId(BoardSource.NEWS);

        // 공지: ADMIN 만 작성, 사이드바 공지사항과 같은 데이터(notice 테이블)
        assertThatThrownBy(() -> communityService.write(viewer.getId(),
                new PostRequest(noticeBoard, "일반회원 공지", "본문")))
                .isInstanceOf(BusinessException.class);
        PostDetail notice = communityService.write(admin.getId(),
                new PostRequest(noticeBoard, "점검 안내", "오늘 밤 점검합니다"));
        assertThat(notice.source()).isEqualTo(BoardSource.NOTICE);
        assertThat(noticeRepository.findById(notice.id())).isPresent();
        assertThat(communityService.list(noticeBoard, "new", null, 0, 20, null).items())
                .anyMatch(p -> p.id().equals(notice.id()));
        assertThat(communityService.noticeDetail(notice.id(), null).title()).isEqualTo("점검 안내");

        // 뉴스: REPORTER 이상만 작성, /news 기사와 같은 데이터(post category=NEWS)
        assertThatThrownBy(() -> communityService.write(viewer.getId(),
                new PostRequest(newsBoard, "일반회원 기사", "본문")))
                .isInstanceOf(BusinessException.class);
        PostDetail news = communityService.write(admin.getId(),
                new PostRequest(newsBoard, "신작 소식", "기사 본문"));
        assertThat(newsRepository.findById(news.id())).isPresent();
        assertThat(communityService.list(newsBoard, "new", null, 0, 20, null).items())
                .anyMatch(p -> p.id().equals(news.id()));

        // 시스템 게시판은 삭제 불가
        assertThatThrownBy(() -> communityService.deleteBoard(noticeBoard))
                .isInstanceOf(BusinessException.class);
    }

    private Long systemBoardId(BoardSource source) {
        return boardRepository.findAllByOrderBySortOrderAscIdAsc().stream()
                .filter(b -> b.getSource() == source)
                .findFirst().orElseThrow().getId();
    }
}
