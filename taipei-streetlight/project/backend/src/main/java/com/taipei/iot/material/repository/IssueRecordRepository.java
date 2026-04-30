package com.taipei.iot.material.repository;

import com.taipei.iot.material.entity.IssueRecord;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRecordRepository extends JpaRepository<IssueRecord, Long>, TenantScopedRepository {

    List<IssueRecord> findByRequestId(Long requestId);

    void deleteByRequestId(Long requestId);
}
