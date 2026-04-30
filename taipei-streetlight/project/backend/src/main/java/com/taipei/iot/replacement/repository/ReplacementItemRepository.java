package com.taipei.iot.replacement.repository;

import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplacementItemRepository extends JpaRepository<ReplacementItem, Long>, TenantScopedRepository {

    List<ReplacementItem> findByOrderId(Long orderId);
}
