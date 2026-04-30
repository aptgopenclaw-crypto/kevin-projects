package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.AlertConfig;
import com.taipei.iot.smartiot.enums.AlertConfigType;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlertConfigRepository extends JpaRepository<AlertConfig, Long>, TenantScopedRepository {

    Optional<AlertConfig> findByTenantIdAndConfigType(String tenantId, AlertConfigType configType);
}
