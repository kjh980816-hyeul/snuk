package com.chzikon.auth.controller;

import com.chzikon.auth.dto.MeResponse;
import com.chzikon.auth.dto.TokenPair;
import com.chzikon.auth.service.AuthService;
import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.member.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final MemberService memberService;

    @Value("${app.frontend.redirect-base}")
    private String frontendRedirectBase;

    /** OAuth 시작(치지직/씨미/숲) → 인가 URL 로 리다이렉트. */
    @GetMapping("/oauth2/authorization/{provider}")
    public void start(@PathVariable String provider, HttpServletResponse response) throws IOException {
        response.sendRedirect(authService.buildAuthorizationUrl(
                com.chzikon.member.domain.Provider.fromPath(provider)));
    }

    /**
     * 콜백 → 토큰 발급 후 프론트로 리다이렉트.
     * 토큰은 URL fragment(#)로 전달(서버 로그/Referer 비노출). 프론트가 저장 후 URL 정리.
     */
    @GetMapping("/login/oauth2/code/{provider}")
    public void callback(@PathVariable String provider,
                         @RequestParam String code,
                         @RequestParam(required = false) String state,
                         HttpServletResponse response) throws IOException {
        try {
            TokenPair tokens = authService.handleCallback(
                    com.chzikon.member.domain.Provider.fromPath(provider), code, state);
            String target = frontendRedirectBase + "/oauth/callback#accessToken="
                    + enc(tokens.accessToken()) + "&refreshToken=" + enc(tokens.refreshToken());
            response.sendRedirect(target);
        } catch (Exception e) {
            log.warn("OAuth callback failed: {}", e.getMessage());
            response.sendRedirect(frontendRedirectBase + "/oauth/callback#error=oauth_failed");
        }
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(authService.refresh(body.get("refreshToken")));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWT — 클라이언트가 토큰 폐기. (확장 시 refresh 블랙리스트)
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(MeResponse.from(memberService.getById(principal.memberId())));
    }

    /** 프사 파일 업로드(이미지만, 5MB 이하). */
    @PostMapping("/api/auth/me/profile-image")
    public ResponseEntity<MeResponse> uploadProfileImage(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @AuthenticationPrincipal MemberPrincipal principal) {
        return ResponseEntity.ok(MeResponse.from(
                memberService.changeProfileImageUpload(principal.memberId(), file)));
    }

    /** 프사 변경(URL). imageUrl 이 비어있으면 치지직 프사로 복원. */
    @PatchMapping("/api/auth/me/profile-image")
    public ResponseEntity<MeResponse> changeProfileImage(@RequestBody Map<String, String> body,
                                                         @AuthenticationPrincipal MemberPrincipal principal) {
        String url = body.get("imageUrl");
        var member = (url == null || url.isBlank())
                ? memberService.resetProfileImage(principal.memberId())
                : memberService.changeProfileImage(principal.memberId(), url.trim());
        return ResponseEntity.ok(MeResponse.from(member));
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }
}
