package com.taipei.iot.repair.listener;

import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * E4：報修派工 → 設備狀態→維修中
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairDispatchedListener {

    private final DeviceService deviceService;
    private final RepairTicketRepository repairTicketRepository;

    @EventListener
    @Transactional
    public void onRepairDispatched(WorkflowTransitionEvent event) {
        if (!"REPAIR_DISPATCH".equals(event.getInstance().getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        Long ticketId = event.getInstance().getTicketId();
        log.info("E4 事件：報修工單 {} 已派工，更新設備狀態為維修中", ticketId);

        RepairTicket ticket = repairTicketRepository.findById(ticketId).orElse(null);
        if (ticket != null && ticket.getDeviceId() != null) {
            deviceService.updateStatus(ticket.getDeviceId(), DeviceStatus.UNDER_REPAIR);
        }
    }
}
