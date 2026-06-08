package com.taipei.iot.workflow.repository;

import com.taipei.iot.workflow.entity.DelegateSetting;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DelegateSettingRepository extends JpaRepository<DelegateSetting, Long>, TenantScopedRepository {

	@Query("""
			SELECT d FROM DelegateSetting d
			WHERE d.delegatorId = :delegatorId
			  AND d.isActive = true
			  AND d.startDate <= :today
			  AND d.endDate >= :today
			""")
	Optional<DelegateSetting> findActiveByDelegator(@Param("delegatorId") String delegatorId,
			@Param("today") LocalDate today);

	@Query("""
			SELECT d FROM DelegateSetting d
			WHERE d.delegateId = :delegateId
			  AND d.isActive = true
			  AND d.startDate <= :today
			  AND d.endDate >= :today
			""")
	List<DelegateSetting> findActiveDelegationsForDelegate(@Param("delegateId") String delegateId,
			@Param("today") LocalDate today);

	@Query("""
			SELECT COUNT(d) > 0 FROM DelegateSetting d
			WHERE d.delegatorId = :delegatorId
			  AND d.isActive = true
			  AND d.startDate <= :endDate
			  AND d.endDate >= :startDate
			""")
	boolean hasOverlappingDelegation(@Param("delegatorId") String delegatorId, @Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate);

	List<DelegateSetting> findByDelegatorIdOrderByCreatedAtDesc(String delegatorId);

}
