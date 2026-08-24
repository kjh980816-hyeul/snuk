package com.chzikon.crew;

import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.security.MemberPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 크루 페이지 콘텐츠(V25) — 조회는 공개, 저장은 ADMIN.
 * /crew/<slug>/ 정적 HTML이 로드 시 GET으로 최신 데이터를 받고,
 * 페이지 내 관리자 화면에서 PUT으로 저장하면 즉시 전 방문자에게 반영된다.
 */
@RestController
@RequiredArgsConstructor
public class CrewPageController {

    private static final Pattern SLUG = Pattern.compile("^[a-z0-9-]{1,100}$");

    private final CrewPageRepository repository;
    private final ObjectMapper objectMapper;

    public record CrewPageView(String slug, String data, LocalDateTime updatedAt) {
        static CrewPageView from(CrewPage p) {
            return new CrewPageView(p.getSlug(), p.getData(), p.getUpdatedAt());
        }
    }

    /** 공개 조회 — 저장 전에는 data=null 로 200(콘솔 404 노이즈 방지, 페이지는 내장 기본 데이터로 폴백). */
    @GetMapping("/api/crew/{slug}")
    public ResponseEntity<CrewPageView> get(@PathVariable String slug) {
        if (!SLUG.matcher(slug).matches()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(repository.findById(slug)
                .map(CrewPageView::from)
                .orElseGet(() -> new CrewPageView(slug, null, null)));
    }

    /** 어드민 저장 — body {"data": "<JSON 문자열>"}. JSON 파싱 검증 후 upsert. */
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/admin/crew/{slug}")
    public ResponseEntity<Map<String, Object>> save(@PathVariable String slug,
                                                    @RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal MemberPrincipal principal) {
        return doSave(slug, body, principal.memberId());
    }

    // ---------- 크루 자체 관리자 로그인(V26) — 스눅 계정 없이 크루 전용 아이디/비밀번호로 저장 ----------

    /** 로그인 — body {"id","pw"}. 성공 시 30일짜리 크루 토큰 발급(HMAC, 비밀번호 변경 시 자동 무효). */
    @PostMapping("/api/crew/{slug}/login")
    public ResponseEntity<Map<String, Object>> login(@PathVariable String slug,
                                                     @RequestBody Map<String, String> body) {
        CrewPage page = findWithLogin(slug);
        String id = body.getOrDefault("id", "");
        String pw = body.getOrDefault("pw", "");
        if (!page.getLoginId().equals(id) || !sha256Hex(slug + ":" + pw).equals(page.getLoginPwHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        long exp = Instant.now().plus(30, ChronoUnit.DAYS).getEpochSecond();
        String token = slug + "." + exp + "." + hmacHex(page.getLoginPwHash(), slug + "." + exp);
        return ResponseEntity.ok(Map.of("token", token, "expiresAt", exp));
    }

    /** 크루 토큰 저장 — 헤더 X-Crew-Token. 어드민 PUT 과 동일 검증/업서트(updated_by 는 비움). */
    @PutMapping("/api/crew/{slug}")
    public ResponseEntity<Map<String, Object>> saveWithCrewToken(@PathVariable String slug,
                                                                 @RequestHeader(value = "X-Crew-Token", required = false) String token,
                                                                 @RequestBody Map<String, String> body) {
        CrewPage page = findWithLogin(slug);
        if (token == null || !verifyCrewToken(page, slug, token)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "크루 관리자 로그인이 필요합니다.");
        }
        return doSave(slug, body, null);
    }

    private ResponseEntity<Map<String, Object>> doSave(String slug, Map<String, String> body, Long memberId) {
        if (!SLUG.matcher(slug).matches()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "크루 주소는 영문 소문자·숫자·하이픈만 가능합니다.");
        }
        String data = body.get("data");
        if (data == null || data.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "저장할 내용이 없습니다.");
        }
        try {
            objectMapper.readTree(data);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "데이터 형식(JSON)이 올바르지 않습니다.");
        }
        CrewPage page = repository.findById(slug)
                .map(p -> {
                    p.update(data, memberId);
                    return p;
                })
                .orElseGet(() -> new CrewPage(slug, data, memberId));
        repository.save(page);
        return ResponseEntity.ok(Map.of("slug", slug, "updatedAt", page.getUpdatedAt()));
    }

    private CrewPage findWithLogin(String slug) {
        if (!SLUG.matcher(slug).matches()) {
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        CrewPage page = repository.findById(slug)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (page.getLoginId() == null || page.getLoginPwHash() == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "이 크루에는 자체 로그인이 설정돼 있지 않습니다.");
        }
        return page;
    }

    private boolean verifyCrewToken(CrewPage page, String slug, String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3 || !parts[0].equals(slug)) {
            return false;
        }
        long exp;
        try {
            exp = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (exp < Instant.now().getEpochSecond()) {
            return false;
        }
        String expected = hmacHex(page.getLoginPwHash(), parts[0] + "." + parts[1]);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                parts[2].getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String s) {
        try {
            return toHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hmacHex(String key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return toHex(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
