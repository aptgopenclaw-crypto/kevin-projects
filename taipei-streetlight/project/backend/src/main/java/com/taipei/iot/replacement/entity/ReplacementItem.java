package com.taipei.iot.replacement.entity;

import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.tenant.TenantAware;
import com.taipei.iot.tenant.TenantEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "replacement_items")
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@EntityListeners({TenantEntityListener.class, AuditingEntityListener.class})
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ReplacementItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "parent_device_id", nullable = false)
    private Long parentDeviceId;

    @Column(name = "old_device_id", nullable = false)
    private Long oldDeviceId;

    @Column(name = "new_device_id")
    private Long newDeviceId;

    @Column(name = "before_device_type", length = 30)
    private String beforeDeviceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_spec", columnDefinition = "jsonb")
    private Map<String, Object> beforeSpec;

    @Column(name = "after_device_type", length = 30)
    private String afterDeviceType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_spec", columnDefinition = "jsonb")
    private Map<String, Object> afterSpec;

    @Column(name = "material_spec_id")
    private Long materialSpecId;

    @Column(name = "approved_material_id")
    private Long approvedMaterialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReplacementItemStatus status;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "completed_by", length = 50)
    private String completedBy;

    @Column(name = "notes")
    private String notes;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String getTenantId() { return tenantId; }

    @Override
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
}
