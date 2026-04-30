package com.taipei.iot.repair.listener;

import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.fault.entity.FaultTicket;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.repair.service.RepairTicketService;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * E1：障礙審核通過 → 自動建立報修工單 + 設備狀態→已報修
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FaultApprovedListener {

    private final RepairTicketService repairTicketService;
    private final DeviceService deviceService;
    private final FaultTicketRepository faultTicketRepository;

    @EventListener
    @Transactional
    public void onFaultConfirmed(WorkflowTransitionEvent event) {
        if (!"FAULT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"CONFIRMED".equals(event.getTargetStep())) return;

        Long faultTicketId = event.getInstance().getTicketId();
        log.info("E1 事件：障礙工單 {} 審核通過，自動建立報修工單", faultTicketId);

        // 1. 自動建立報修工單
        repairTicketService.createFromFault(faultTicketId);

        // 2. 設備狀態 → 已報修
        FaultTicket fault = faultTicketRepository.findById(faultTicketId).orElse(null);
        if (fault != null && fault.getDeviceId() != null) {
            deviceService.updateStatus(fault.getDeviceId(), DeviceStatus.REPORTED);
        }
    }
}
