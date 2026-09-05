package com.chzikon.community.repository;

import com.chzikon.community.domain.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    /** 특정 게시판(하위 게시판 포함) 검색. */
    @Query("""
            select p from CommunityPost p
            where p.hidden = false
              and p.boardId in :boardIds
              and (:q is null or lower(p.title) like :q or lower(p.content) like :q)
            """)
    Page<CommunityPost> searchInBoards(@Param("boardIds") Collection<Long> boardIds,
                                       @Param("q") String q,
                                       Pageable pageable);

    /** 인기글 — 댓글 가중치 3 + 조회수/50 (데모 시안 기준). */
    @Query("""
            select p from CommunityPost p
            where p.hidden = false
              and (:allBoards = true or p.boardId in :boardIds)
            order by (p.commentCount * 3 + p.viewCount / 50) desc, p.createdAt desc
            """)
    List<CommunityPost> findPopular(@Param("allBoards") boolean allBoards,
                                    @Param("boardIds") Collection<Long> boardIds,
                                    Pageable pageable);

    List<CommunityPost> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<CommunityPost> findTop200ByOrderByCreatedAtDesc();

    /** 같은 게시판의 다른 글. */
    List<CommunityPost> findTop5ByBoardIdAndHiddenFalseAndIdNotOrderByCreatedAtDesc(Long boardId, Long id);

    boolean existsByBoardId(Long boardId);

    @Modifying(clearAutomatically = true)
    @Query("update CommunityPost p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long id);
}
