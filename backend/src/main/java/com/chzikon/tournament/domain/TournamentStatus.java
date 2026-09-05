package com.chzikon.tournament.domain;

/**
 * 대회 상태. PREPARING=준비중(내용만 공개·신청 불가) → SCHEDULED=오픈예정 → OPEN=모집중 →
 * CLOSED=모집 마감(대회 진행) → DONE=종료(result_text 노출).
 */
public enum TournamentStatus {
    PREPARING, SCHEDULED, OPEN, CLOSED, DONE
}
