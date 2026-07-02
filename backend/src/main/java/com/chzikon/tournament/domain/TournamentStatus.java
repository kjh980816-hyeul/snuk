package com.chzikon.tournament.domain;

/** 대회 상태. DONE 은 종료 + 결과(result_text) 노출 단계. */
public enum TournamentStatus {
    SCHEDULED, OPEN, CLOSED, DONE
}
