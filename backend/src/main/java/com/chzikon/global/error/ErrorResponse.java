package com.chzikon.global.error;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        List<FieldErrorDetail> errors
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(),
                errorCode.getCode(), errorCode.getMessage(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(),
                errorCode.getCode(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorDetail> errors) {
        return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(),
                errorCode.getCode(), errorCode.getMessage(), errors);
    }

    public record FieldErrorDetail(String field, String reason) {
    }
}
