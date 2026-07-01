package com.chzikon.admin.controller;

import com.chzikon.admin.domain.AdminLog;
import com.chzikon.admin.domain.AppSetting;
import com.chzikon.admin.service.AdminLogService;
import com.chzikon.admin.service.AppSettingService;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import com.chzikon.global.security.MemberPrincipal;
import com.chzikon.member.domain.Member;
import com.chzikon.member.domain.Role;
import com.chzikon.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 어드민 설정·권한 오버라이드·감사 로그. */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AppSettingService appSettingService;
    private final AdminLogService adminLogService;
    private final MemberService memberService;

    // ----- 설정값 -----
    @GetMapping("/settings")
    public ResponseEntity<List<AppSetting>> settings() {
        return ResponseEntity.ok(appSettingService.findAll());
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<AppSetting> updateSetting(@PathVariable String key,
                                                    @RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal MemberPrincipal principal) {
        String value = body.get("value");
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "value 가 필요합니다.");
        }
        AppSetting saved = appSettingService.upsert(key, value, principal.memberId());
        adminLogService.record(principal.memberId(), "SETTING_UPDATE", "app_setting", null,
                key + "=" + value);
        return ResponseEntity.ok(saved);
    }

    // ----- 권한 수동 오버라이드 (AUTH-05) -----
    @PostMapping("/members/{id}/role-override")
    public ResponseEntity<Map<String, Object>> overrideRole(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body,
                                                            @AuthenticationPrincipal MemberPrincipal principal) {
        Role role = parseRole(body.get("role"));
        Member member = memberService.overrideRole(id, role);
        adminLogService.record(principal.memberId(), "ROLE_OVERRIDE", "member", id, "role=" + role);
        return ResponseEntity.ok(Map.of("memberId", member.getId(), "role", member.getRole().name(),
                "roleOverridden", member.isRoleOverridden()));
    }

    @DeleteMapping("/members/{id}/role-override")
    public ResponseEntity<Map<String, Object>> clearOverride(@PathVariable Long id,
                                                             @AuthenticationPrincipal MemberPrincipal principal) {
        Member member = memberService.clearOverride(id);
        adminLogService.record(principal.memberId(), "ROLE_OVERRIDE_CLEAR", "member", id, null);
        return ResponseEntity.ok(Map.of("memberId", member.getId(),
                "roleOverridden", member.isRoleOverridden()));
    }

    // ----- 감사 로그 -----
    @GetMapping("/logs")
    public ResponseEntity<Page<AdminLog>> logs(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(adminLogService.findRecent(PageRequest.of(page, size)));
    }

    private Role parseRole(String raw) {
        if (raw == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "role 이 필요합니다.");
        }
        try {
            Role role = Role.valueOf(raw.trim().toUpperCase());
            if (role == Role.GUEST) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "GUEST 로는 지정할 수 없습니다.");
            }
            return role;
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "알 수 없는 role: " + raw);
        }
    }
}
