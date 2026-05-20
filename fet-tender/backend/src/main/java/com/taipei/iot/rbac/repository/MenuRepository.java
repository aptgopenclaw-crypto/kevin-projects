package com.taipei.iot.rbac.repository;

import com.taipei.iot.rbac.entity.MenuEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface MenuRepository extends JpaRepository<MenuEntity, Long> {

    List<MenuEntity> findByParentIdOrderBySortOrder(Long parentId);

    List<MenuEntity> findAllByOrderBySortOrder();

    List<MenuEntity> findByPermissionCodeInAndVisibleTrue(Collection<String> codes);

    /** 無需權限、所有登入使用者都可看到的選單（permission_code IS NULL） */
    List<MenuEntity> findByPermissionCodeIsNullAndVisibleTrue();

    boolean existsByParentId(Long parentId);
}
