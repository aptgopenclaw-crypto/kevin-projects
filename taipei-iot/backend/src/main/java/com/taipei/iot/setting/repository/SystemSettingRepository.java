package com.taipei.iot.setting.repository;

import com.taipei.iot.setting.entity.SystemSettingEntity;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SystemSettingRepository extends JpaRepository<SystemSettingEntity, Long>, TenantScopedRepository {

    Optional<SystemSettingEntity> findBySettingKey(String settingKey);
}
