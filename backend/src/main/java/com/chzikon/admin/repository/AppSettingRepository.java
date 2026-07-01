package com.chzikon.admin.repository;

import com.chzikon.admin.domain.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, String> {

    Optional<AppSetting> findBySettingKey(String settingKey);

    @Override
    List<AppSetting> findAll();
}
