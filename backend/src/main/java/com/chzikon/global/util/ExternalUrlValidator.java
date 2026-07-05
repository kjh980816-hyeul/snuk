package com.chzikon.global.util;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

/**
 * 외부 URL 입력 검증(배너·게임링크·후기링크·이미지). security.md:
 * - https:// 시작 강제, javascript: 스킴 차단
 * - 내부 IP/메타데이터(169.254.169.254 등) 차단 (SSRF/XSS 방어)
 * null/blank 은 허용(선택 필드) — 호출측에서 필요 시 필수 검증.
 */
@Component
public class ExternalUrlValidator {

    public void validateNullable(String url) {
        if (url == null || url.isBlank()) {
            return;
        }
        validate(url);
    }

    public void validate(String url) {
        String lower = url.trim().toLowerCase(Locale.ROOT);
        // 내부 업로드 파일 경로(/uploads/…)는 우리 서버 정적 파일 — 외부 요청이 없어 SSRF 무관
        if (lower.startsWith("/uploads/") && !lower.contains("..")) {
            return;
        }
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "허용되지 않는 URL 스킴입니다.");
        }
        if (!lower.startsWith("https://")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "https:// 로 시작하는 URL 만 허용됩니다.");
        }
        final String host;
        try {
            host = URI.create(url.trim()).getHost();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "URL 형식이 올바르지 않습니다.");
        }
        if (host == null || host.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "URL 호스트가 없습니다.");
        }
        if (isBlockedHost(host.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "내부/사설 주소는 허용되지 않습니다.");
        }
    }

    private boolean isBlockedHost(String host) {
        if (host.equals("localhost") || host.endsWith(".localhost") || host.endsWith(".internal")) {
            return true;
        }
        // 클라우드 메타데이터 / 사설망 / 루프백 대역
        return host.equals("169.254.169.254")
                || host.startsWith("127.")
                || host.startsWith("10.")
                || host.startsWith("192.168.")
                || host.startsWith("0.")
                || isPrivate172(host);
    }

    private boolean isPrivate172(String host) {
        if (!host.startsWith("172.")) {
            return false;
        }
        String[] parts = host.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31; // 172.16.0.0 ~ 172.31.255.255
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
