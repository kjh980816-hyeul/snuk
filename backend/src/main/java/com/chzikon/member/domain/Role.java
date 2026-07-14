package com.chzikon.member.domain;

/**
 * 권한 등급 (AUTH-03).
 * GUEST   - 비로그인 (보통 미저장)
 * VIEWER  - 로그인했으나 채널 없음 또는 팔로워 < 임계값
 * STREAMER- 팔로워 ≥ 임계값 (캠페인 신청 자격)
 * REPORTER- 스눅 뉴스 기자(수동 지정). 뉴스 작성 가능, 어드민 기능 불가
 * ADMIN   - 대표
 */
public enum Role {
    GUEST,
    VIEWER,
    STREAMER,
    REPORTER,
    ADMIN;

    /** STREAMER 이상 (캠페인 신청 자격) */
    public boolean isStreamerOrAbove() {
        return this == STREAMER || this == REPORTER || this == ADMIN;
    }

    /** REPORTER 이상 (스눅 뉴스 작성 자격) */
    public boolean isReporterOrAbove() {
        return this == REPORTER || this == ADMIN;
    }

    /** Spring Security 권한 문자열 */
    public String authority() {
        return "ROLE_" + name();
    }
}
