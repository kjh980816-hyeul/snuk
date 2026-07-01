package com.chzikon.campaign.dto;

import jakarta.validation.constraints.NotBlank;

/** 키 일괄 등록(붙여넣기). 줄바꿈 구분 텍스트. */
public record KeyRegisterRequest(
        @NotBlank String rawKeys
) {
}
