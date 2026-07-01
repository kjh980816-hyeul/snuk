package com.chzikon.auth.service;

import com.chzikon.admin.service.AppSettingService;
import com.chzikon.member.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 팔로워 수 → 권한 자동 산정 (AUTH-03/04). 임계값은 app_setting 에서 읽는다. */
@Component
@RequiredArgsConstructor
public class RoleCalculator {

    private final AppSettingService appSettingService;

    /**
     * @param followerCount nullable (채널 없음/조회 실패 → VIEWER 폴백, AUTH-07)
     * @return STREAMER (팔로워 ≥ 임계값) 또는 VIEWER
     */
    public Role compute(Integer followerCount) {
        if (followerCount == null) {
            return Role.VIEWER;
        }
        int threshold = appSettingService.getStreamerFollowerThreshold();
        return followerCount >= threshold ? Role.STREAMER : Role.VIEWER;
    }
}
