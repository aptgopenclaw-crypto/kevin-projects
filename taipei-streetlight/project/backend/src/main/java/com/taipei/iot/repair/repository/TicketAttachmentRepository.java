package com.taipei.iot.repair.repository;

import com.taipei.iot.repair.entity.TicketAttachment;
import com.taipei.iot.repair.enums.TicketType;
import com.taipei.iot.tenant.TenantScopedRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long>, TenantScopedRepository {

    List<TicketAttachment> findByTicketTypeAndTicketIdOrderByUploadedAtDesc(TicketType ticketType, Long ticketId);
}
