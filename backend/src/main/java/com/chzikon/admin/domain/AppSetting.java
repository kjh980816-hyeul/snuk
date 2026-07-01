package com.chzikon.admin.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** 어드민 설정값(key-value). 팔로워 임계값 등. 코드 상수 금지(ADR-003). */
@Entity
@Table(name = "app_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppSetting {

    @Id
    @Column(name = "setting_key", length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 512)
    private String settingValue;

    @Column(length = 255)
    private String description;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AppSetting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    public void update(String value, Long updatedBy) {
        this.settingValue = value;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
