package com.chzikon.notice.repository;

import com.chzikon.notice.domain.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /** 고정 우선 + 최신순. */
    @Query("SELECT n FROM Notice n ORDER BY n.pinned DESC, n.createdAt DESC")
    List<Notice> findLatest(Pageable pageable);
}
