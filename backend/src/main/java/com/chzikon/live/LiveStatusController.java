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
 * 라이브 여부 — 공개 엔드포인트.
 * 치지직 서비스 API live-detail(v2, v1 은 9004 차단)로 status OPEN 여부만 조회.
 * /status    : 공식 채널(LIVE_CHANNEL_ID) 단건. 30초 캐시.
 * /streamers : 파트너 스트리머 일괄(라이브 우선 정렬용, 항목 7). 60초 캐시.
 * /hls       : 채널 1개의 HLS 재생 주소(livePlaybackJson) — 홈 무대·/live 가 hls.js 로 직접 재생. 20초 캐시(채널별).
 *              (치지직 CDN 은 CORS * 라 브라우저 직재생 가능. 19금/비공개는 재생주소가 없어 hlsUrl=null → 프론트가 크롭 폴백)
 * 외부 API 실패/채널 미설정('-') 시 live=false.
 */
@Slf4j
@RestController
@RequestMapping("/api/live")
@RequiredArgsConstructor
public class LiveStatusController {

    private static final long CACHE_MS = 30_000;
    private static final long STREAMERS_CACHE_MS = 60_000;

    private final AppSettingService appSettingService;
    private final com.chzikon.member.repository.MemberRepository memberRepository;
    private final RestClient restClient = RestClient.create();

    private volatile long cachedAt = 0;
    private volatile boolean cachedLive = false;
    private volatile String cachedTitle = "";

    private static final long HLS_CACHE_MS = 20_000;
    private static final java.util.regex.Pattern CHANNEL_ID = java.util.regex.Pattern.compile("^[0-9a-f]{32}$");
    private final java.util.concurrent.ConcurrentHashMap<String, Map<String, Object>> hlsCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> hlsCachedAt = new java.util.concurrent.ConcurrentHashMap<>();

    private volatile long streamersCachedAt = 0;
    private volatile java.util.List<Map<String, Object>> cachedStreamers = java.util.List.of();

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        long now = System.currentTimeMillis();
        if (now - cachedAt > CACHE_MS) {
            refresh();
            cachedAt = now;
        }
        return ResponseEntity.ok(Map.of("live", cachedLive, "liveTitle", cachedTitle));
    }

    /** 파트너 스트리머(관리자 지정, CHZZK) 라이브 여부 일괄 — [{memberId, live, liveTitle}]. */
    @GetMapping("/streamers")
    public ResponseEntity<java.util.List<Map<String, Object>>> streamers() {
        long now = System.currentTimeMillis();
        if (now - streamersCachedAt > STREAMERS_CACHE_MS) {
            refreshStreamers();
            streamersCachedAt = now;
        }
        return ResponseEntity.ok(cachedStreamers);
    }

    /** 채널 HLS 재생 정보 — {live, hlsUrl, title, viewers, adult}. 채널 ID 형식 불일치 400. */
    @GetMapping("/hls")
    public ResponseEntity<Map<String, Object>> hls(@org.springframework.web.bind.annotation.RequestParam("channel") String channel) {
        String id = channel == null ? "" : channel.trim().toLowerCase();
        if (!CHANNEL_ID.matcher(id).matches()) {
            return ResponseEntity.badRequest().body(Map.of("live", false, "error", "invalid channel"));
        }
        long now = System.currentTimeMillis();
        Long at = hlsCachedAt.get(id);
        Map<String, Object> cached = hlsCache.get(id);
        if (cached == null || at == null || now - at > HLS_CACHE_MS) {
            cached = fetchHls(id);
            hlsCache.put(id, cached);
            hlsCachedAt.put(id, now);
            if (hlsCache.size() > 200) { hlsCache.clear(); hlsCachedAt.clear(); } // 무한 증식 방지
        }
        return ResponseEntity.ok(cached);
    }

    private Map<String, Object> fetchHls(String id) {
        Map<String, Object> out = new java.util.HashMap<>();
        out.put("live", false);
        try {
            JsonNode body = restClient.get()
                    .uri("https://api.chzzk.naver.com/service/v2/channels/{id}/live-detail", id)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = body == null ? null : body.path("content");
            if (content == null || content.isMissingNode() || content.isNull()) return out;
            boolean live = "OPEN".equals(content.path("status").asText());
            out.put("live", live);
            out.put("title", content.path("liveTitle").asText(""));
            out.put("viewers", content.path("concurrentUserCount").asInt(0));
            out.put("adult", content.path("adult").asBoolean(false));
            out.put("channelName", content.path("channel").path("channelName").asText(""));
            String pbRaw = content.path("livePlaybackJson").isNull() ? null : content.path("livePlaybackJson").asText(null);
            if (live && pbRaw != null && !pbRaw.isBlank()) {
                JsonNode pb = new com.fasterxml.jackson.databind.ObjectMapper().readTree(pbRaw);
                String hls = null, llhls = null;
                for (JsonNode m : pb.path("media")) {
                    String mid = m.path("mediaId").asText("");
                    String path = m.path("path").asText("");
                    if (path.isBlank()) continue;
                    if ("HLS".equals(mid) && hls == null) hls = path;
                    else if ("LLHLS".equals(mid) && llhls == null) llhls = path;
                }
                String pick = hls != null ? hls : llhls;
                if (pick != null) out.put("hlsUrl", pick);
            }
        } catch (Exception e) {
            log.debug("hls fetch failed ({}): {}", id, e.getMessage());
        }
        return out;
    }

    private void refreshStreamers() {
        try {
            var members = memberRepository.findTop60ByPartnerTrueOrderByFollowerCountDesc();
            java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
            for (var m : members) {
                if (m.getProvider() != com.chzikon.member.domain.Provider.CHZZK) {
                    continue; // 치지직만 라이브 조회 가능
                }
                boolean live = false;
                String title = "";
                try {
                    JsonNode body = restClient.get()
                            .uri("https://api.chzzk.naver.com/service/v2/channels/{id}/live-detail", m.getChannelId())
                            .header("User-Agent", "Mozilla/5.0")
                            .retrieve()
                            .body(JsonNode.class);
                    JsonNode content = body == null ? null : body.path("content");
                    live = content != null && "OPEN".equals(content.path("status").asText());
                    title = content == null ? "" : content.path("liveTitle").asText("");
                } catch (Exception e) {
                    log.debug("streamer live fetch failed ({}): {}", m.getChannelId(), e.getMessage());
                }
                if (live) {
                    out.add(Map.of("memberId", m.getId(), "live", true, "liveTitle", title));
                }
            }
            cachedStreamers = java.util.List.copyOf(out);
        } catch (Exception e) {
            log.debug("streamers live refresh failed: {}", e.getMessage());
            cachedStreamers = java.util.List.of();
        }
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
