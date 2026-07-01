package com.chzikon.campaign.dto;

import com.chzikon.campaign.domain.GameKey;
import com.chzikon.global.crypto.KeyCipher;

import java.time.LocalDateTime;

/** 어드민 키 목록 — 마스킹된 형태만(평문 금지, security.md). */
public record GameKeyAdminView(
        Long id,
        String maskedKey,
        String status,
        Long assignedMemberId,
        LocalDateTime assignedAt
) {
    public static GameKeyAdminView of(GameKey key, String plainForMaskOnly) {
        return new GameKeyAdminView(
                key.getId(),
                KeyCipher.mask(plainForMaskOnly),
                key.getStatus().name(),
                key.getAssignedMemberId(),
                key.getAssignedAt());
    }
}
