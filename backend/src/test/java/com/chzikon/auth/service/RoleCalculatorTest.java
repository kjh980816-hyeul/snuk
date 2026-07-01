package com.chzikon.auth.service;

import com.chzikon.admin.service.AppSettingService;
import com.chzikon.member.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RoleCalculatorTest {

    private final AppSettingService appSettingService = mock(AppSettingService.class);
    private final RoleCalculator calculator = new RoleCalculator(appSettingService);

    @Test
    void null_follower_falls_back_to_viewer() {
        assertThat(calculator.compute(null)).isEqualTo(Role.VIEWER);
    }

    @Test
    void at_threshold_is_streamer_boundary() {
        when(appSettingService.getStreamerFollowerThreshold()).thenReturn(50);
        assertThat(calculator.compute(49)).isEqualTo(Role.VIEWER);
        assertThat(calculator.compute(50)).isEqualTo(Role.STREAMER); // 경계 포함(≥)
        assertThat(calculator.compute(51)).isEqualTo(Role.STREAMER);
    }

    @Test
    void threshold_change_reflected() {
        when(appSettingService.getStreamerFollowerThreshold()).thenReturn(100);
        assertThat(calculator.compute(50)).isEqualTo(Role.VIEWER);
    }
}
