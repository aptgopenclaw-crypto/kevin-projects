package com.taipei.iot.device.repository;

import com.taipei.iot.device.entity.DeviceTemplate;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceTemplateRepository extends JpaRepository<DeviceTemplate, Long>, TenantScopedRepository {

    Optional<DeviceTemplate> findByDeviceType(String deviceType);
}
