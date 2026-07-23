package com.chzikon.admin.service;

import com.chzikon.admin.domain.AppSetting;
import com.chzikon.admin.repository.AppSettingRepository;
import com.chzikon.global.error.BusinessException;
import com.chzikon.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppSettingService {

    public static final String STREAMER_FOLLOWER_THRESHOLD = "STREAMER_FOLLOWER_THRESHOLD";
    private static final int DEFAULT_THRESHOLD = 50;

    private final AppSettingRepository repository;

    @Transactional(readOnly = true)
    public int getStreamerFollowerThreshold() {
        return repository.findBySettingKey(STREAMER_FOLLOWER_THRESHOLD)
                .map(s -> parseIntSafe(s.getSettingValue(), DEFAULT_THRESHOLD))
                .orElse(DEFAULT_THRESHOLD);
    }

    /** 숫자 설정 조회(미설정/파싱 실패 시 fallback). */
    @Transactional(readOnly = true)
    public int getInt(String key, int fallback) {
        return repository.findBySettingKey(key)
                .map(s -> parseIntSafe(s.getSettingValue(), fallback))
                .orElse(fallback);
    }

    @Transactional(readOnly = true)
    public List<AppSetting> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public String get(String key) {
        return repository.findBySettingKey(key)
                .map(AppSetting::getSettingValue)
                .orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
    }

    @Transactional
    public AppSetting upsert(String key, String value, Long updatedBy) {
        AppSetting setting = repository.findBySettingKey(key)
                .orElseGet(() -> new AppSetting(key, value, null));
        setting.update(value, updatedBy);
        return repository.save(setting);
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
