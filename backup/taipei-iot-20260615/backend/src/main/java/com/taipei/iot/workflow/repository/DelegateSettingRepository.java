package com.taipei.iot.workflow.repository;

import com.taipei.iot.tenant.TenantScopedRepository;
import com.taipei.iot.workflow.entity.DelegateSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface DelegateSettingRepository extends JpaRepository<DelegateSettingEntity, Long>, TenantScopedRepository {

	@Query("""
			SELECT d FROM DelegateSettingEntity d
			WHERE d.delegateFor = :delegateFor
			  AND (d.businessType IS NULL OR d.businessType = :businessType)
			  AND d.effectiveFrom <= :today
			  AND d.effectiveTo   >= :today
			ORDER BY d.businessType DESC NULLS LAST
			LIMIT 1
			""")
	Optional<DelegateSettingEntity> findActiveDelegate(@Param("delegateFor") String delegateFor,
			@Param("businessType") String businessType, @Param("today") LocalDate today);

}
