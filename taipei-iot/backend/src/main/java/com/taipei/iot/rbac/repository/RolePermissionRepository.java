package com.taipei.iot.rbac.repository;

import com.taipei.iot.rbac.entity.RolePermissionEntity;
import com.taipei.iot.rbac.entity.RolePermissionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, RolePermissionId> {

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.roleId = :roleId AND (rp.tenantId IS NULL OR rp.tenantId = :tenantId)")
    List<RolePermissionEntity> findByRoleIdAndTenantScope(@Param("roleId") String roleId, @Param("tenantId") String tenantId);

    @Query("SELECT rp FROM RolePermissionEntity rp WHERE rp.roleId IN :roleIds AND (rp.tenantId IS NULL OR rp.tenantId = :tenantId)")
    List<RolePermissionEntity> findByRoleIdInAndTenantScope(@Param("roleIds") Collection<String> roleIds, @Param("tenantId") String tenantId);

    List<RolePermissionEntity> findByRoleId(String roleId);

    void deleteByRoleId(String roleId);
}
