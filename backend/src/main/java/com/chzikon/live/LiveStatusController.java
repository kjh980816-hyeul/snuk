package com.chzikon.live;

import com.chzikon.admin.service.AppSettingService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 공식 채널 라이브 여부 — 공개 엔드포인트.
 * 치지직 서비스 API live-detail(v2, v1 은 9004 차단)로 status OPEN 여부만 조회.
 * 외부 API 실패/채널 미설정('-') 시 live=false. 30초 캐시(과호출 방지).
 */
@Slf4j
@RestController
@RequestMapping("/api/live/status")
@RequiredArgsConstructor
public class LiveStatusController {

    private static final long CACHE_MS = 30_000;

    private final AppSettingService appSettingService;
    private final RestClient restClient = RestClient.create();

    private volatile long cachedAt = 0;
    private volatile boolean cachedLive = false;
    private volatile String cachedTitle = "";

    @GetMapping
    public ResponseEntity<Map<String, Object>> status() {
        long now = System.currentTimeMillis();
        if (now - cachedAt > CACHE_MS) {
            refresh();
            cachedAt = now;
        }
        return ResponseEntity.ok(Map.of("live", cachedLive, "liveTitle", cachedTitle));
    }

    private void refresh() {
        String channelId;
        try {
            channelId = appSettingService.get("LIVE_CHANNEL_ID");
        } catch (Exception e) {
            channelId = "-";
        }
        if (channelId.isBlank() || "-".equals(channelId)) {
            cachedLive = false;
            cachedTitle = "";
            return;
        }
        try {
            JsonNode body = restClient.get()
                    .uri("https://api.chzzk.naver.com/service/v2/channels/{id}/live-detail", channelId)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = body == null ? null : body.path("content");
            cachedLive = content != null && "OPEN".equals(content.path("status").asText());
            cachedTitle = content == null ? "" : content.path("liveTitle").asText("");
        } catch (Exception e) {
            log.debug("live status fetch failed: {}", e.getMessage());
            cachedLive = false;
            cachedTitle = "";
        }
    }
}
