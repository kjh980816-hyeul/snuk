package com.chzikon.review.dto;

import jakarta.validation.constraints.NotBlank;

public record ReviewCreateRequest(
        @NotBlank String title,
        String content
) {
}
