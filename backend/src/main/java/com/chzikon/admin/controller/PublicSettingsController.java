package com.chzikon.admin.controller;

import com.chzikon.admin.domain.AppSetting;
import com.chzikon.admin.service.AppSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 공개 사이트 설정 — 화이트리스트 키만 노출(임계값 등 내부 설정은 제외).
 * 라이브 채널 ID·배너 이미지 등 프론트가 렌더링에 쓰는 값.
 */
@RestController
@RequestMapping("/api/site-settings")
@RequiredArgsConstructor
public class PublicSettingsController {

    private static final Set<String> PUBLIC_KEYS = Set.of(
            "LIVE_CHANNEL_ID", "HERO_IMAGE_URL", "POINT_DAILY_AMOUNT", "SPOTLIGHT_POINT_COST");

    private static boolean isPublic(String key) {
        // BANNER_* = 페이지 배너 이미지·문구(V10/V12), LIVE_BANNER_* = 메인 라이브 배너(V13),
        // MENU_* = 사이드바 메뉴 표시/숨김(V15 항목 8) — 전부 노출용 값
        return PUBLIC_KEYS.contains(key) || key.startsWith("BANNER_")
                || key.startsWith("LIVE_BANNER_") || key.startsWith("MENU_");
    }

    private final AppSettingService appSettingService;

    @GetMapping
    public ResponseEntity<Map<String, String>> publicSettings() {
        Map<String, String> out = new LinkedHashMap<>();
        for (AppSetting s : appSettingService.findAll()) {
            if (isPublic(s.getSettingKey())) {
                out.put(s.getSettingKey(), s.getSettingValue());
            }
        }
        return ResponseEntity.ok(out);
    }
}
