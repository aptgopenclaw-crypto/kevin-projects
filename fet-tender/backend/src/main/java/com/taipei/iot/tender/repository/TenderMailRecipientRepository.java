package com.taipei.iot.tender.repository;

import com.taipei.iot.tender.entity.TenderMailRecipient;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TenderMailRecipientRepository extends JpaRepository<TenderMailRecipient, Long>, TenantScopedRepository {

    List<TenderMailRecipient> findByIsActiveTrueOrderByNameAsc();

    List<TenderMailRecipient> findAllByOrderByNameAsc();
}
