package com.taipei.iot.rbac.repository;

import com.taipei.iot.rbac.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PermissionRepository extends JpaRepository<PermissionEntity, String> {

    List<PermissionEntity> findAllByOrderByGroupNameAscSortOrderAsc();

    List<PermissionEntity> findByCodeIn(Collection<String> codes);
}
