package com.taipei.iot.rbac.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "role_permissions")
@IdClass(RolePermissionId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionEntity {

    @Id
    @Column(name = "role_id", length = 50, nullable = false)
    private String roleId;

    @Id
    @Column(name = "permission_id", length = 50, nullable = false)
    private String permissionId;

    @Column(name = "tenant_id", length = 50)
    private String tenantId;
}
