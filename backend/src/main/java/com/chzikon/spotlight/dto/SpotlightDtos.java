package com.chzikon.spotlight.dto;

import com.chzikon.member.domain.Member;
import com.chzikon.spotlight.domain.Spotlight;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class SpotlightDtos {

    private SpotlightDtos() {
    }

    public record SpotlightCreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotNull Spotlight.Platform platform,
            @NotBlank @Size(max = 512) String streamUrl
    ) {
    }

    /** 공개 노출용 — 등록 스트리머의 닉네임/프사 포함. */
    public record SpotlightResponse(
            Long id,
            String title,
            String platform,
            String streamUrl,
            String streamerName,
            String streamerImageUrl,
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    ) {
        public static SpotlightResponse of(Spotlight s, Member m) {
            return new SpotlightResponse(s.getId(), s.getTitle(), s.getPlatform().name(), s.getStreamUrl(),
                    m != null ? m.getNickname() : "스트리머",
                    m != null ? m.getProfileImageUrl() : null,
                    s.getCreatedAt(), s.getExpiresAt());
        }
    }
}
