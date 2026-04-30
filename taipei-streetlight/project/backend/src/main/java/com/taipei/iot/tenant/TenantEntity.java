package com.taipei.iot.tenant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "tenant")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public class TenantEntity {

    @Id
    @Column(name = "tenant_id", length = 50)
    private String tenantId;

    @Column(name = "tenant_code", length = 50, nullable = false, unique = true)
    private String tenantCode;

    @Column(name = "tenant_name", length = 200, nullable = false)
    private String tenantName;

    @Column(name = "deployment_mode", length = 20, nullable = false)
    private String deploymentMode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private Map<String, Object> config;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled;

    @CreatedDate
    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @LastModifiedDate
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
