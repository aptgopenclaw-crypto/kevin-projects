package com.taipei.iot.smartiot.repository;

import com.taipei.iot.smartiot.entity.TelemetryFormat;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TelemetryFormatRepository extends JpaRepository<TelemetryFormat, Long>, TenantScopedRepository {

    Optional<TelemetryFormat> findByTenantIdAndVendorNameAndDeviceModelAndVersion(
            String tenantId, String vendorName, String deviceModel, Integer version);

    Page<TelemetryFormat> findByEnabledTrue(Pageable pageable);

    @Query("SELECT f FROM TelemetryFormat f WHERE "
         + "(:vendorName IS NULL OR f.vendorName = :vendorName) "
         + "AND (:keyword IS NULL OR f.vendorName LIKE %:keyword% OR f.deviceModel LIKE %:keyword% OR f.description LIKE %:keyword%)")
    Page<TelemetryFormat> findByFilters(@Param("vendorName") String vendorName,
                                        @Param("keyword") String keyword,
                                        Pageable pageable);
}
