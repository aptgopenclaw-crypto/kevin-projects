package com.taipei.iot.repair.repository;

import com.taipei.iot.repair.entity.RepairDispatch;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepairDispatchRepository extends JpaRepository<RepairDispatch, Long>, TenantScopedRepository {

    List<RepairDispatch> findByRepairTicketIdOrderByDispatchedAtDesc(Long repairTicketId);

    void deleteByRepairTicketId(Long repairTicketId);
}
