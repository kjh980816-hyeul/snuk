package com.chzikon.community.domain;

/**
 * 시스템 게시판 연결 대상.
 * NOTICE = notice 테이블(사이드바 공지사항과 같은 데이터)
 * NEWS   = post(category=NEWS) — 스눅 뉴스 기사와 같은 데이터
 */
public enum BoardSource {
    NOTICE,
    NEWS
}
