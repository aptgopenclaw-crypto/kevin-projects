package com.taipei.iot.dept.repository;

import com.taipei.iot.dept.entity.DeptInfoEntity;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeptInfoRepository extends JpaRepository<DeptInfoEntity, Long>, TenantScopedRepository {

    List<DeptInfoEntity> findAllByStatusOrderByDeptSortAsc(Short status);

    Optional<DeptInfoEntity> findByDeptId(Long deptId);

    List<DeptInfoEntity> findByPid(Long pid);

    boolean existsByPid(Long pid);

    boolean existsByTenantIdAndDeptNameAndPid(String tenantId, String deptName, Long pid);

    Optional<DeptInfoEntity> findByTenantIdAndDeptName(String tenantId, String deptName);
}
