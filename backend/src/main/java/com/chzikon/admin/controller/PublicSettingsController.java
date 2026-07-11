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
            "LIVE_CHANNEL_ID", "HERO_IMAGE_URL", "BANNER_GOODS_URL", "BANNER_PARTNERS_URL");

    private final AppSettingService appSettingService;

    @GetMapping
    public ResponseEntity<Map<String, String>> publicSettings() {
        Map<String, String> out = new LinkedHashMap<>();
        for (AppSetting s : appSettingService.findAll()) {
            if (PUBLIC_KEYS.contains(s.getSettingKey())) {
                out.put(s.getSettingKey(), s.getSettingValue());
            }
        }
        return ResponseEntity.ok(out);
    }
}
