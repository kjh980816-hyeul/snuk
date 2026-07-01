package com.chzikon.campaign.dto;

/** 키 일괄 등록 결과 리포트(등록 N / 중복 M / 빈줄 K). 평문 키 비포함. */
public record KeyRegisterResult(
        int registered,
        int duplicated,
        int blank,
        int totalAvailable
) {
}
