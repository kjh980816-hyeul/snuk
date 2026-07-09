package com.chzikon.notice.dto;

import com.chzikon.notice.domain.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class NoticeDtos {

    private NoticeDtos() {
    }

    public record NoticeRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 10_000) String content,
            boolean pinned
    ) {
    }

    public record NoticeResponse(
            Long id,
            String title,
            String content,
            boolean pinned,
            LocalDateTime createdAt
    ) {
        public static NoticeResponse from(Notice n) {
            return new NoticeResponse(n.getId(), n.getTitle(), n.getContent(), n.isPinned(), n.getCreatedAt());
        }
    }
}
