package com.taipei.iot.repair.listener;

import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceEventService;
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
 * E9：結案審核通過 → 設備狀態→正常 + 同步資產歷程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RepairClosedListener {

    private final DeviceEventService deviceEventService;
    private final DeviceService deviceService;
    private final RepairTicketRepository repairTicketRepository;

    @EventListener
    @Transactional
    public void onRepairClosed(WorkflowTransitionEvent event) {
        if (!"REPAIR_CLOSE".equals(event.getInstance().getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        Long ticketId = event.getInstance().getTicketId();
        log.info("E9 事件：報修工單 {} 結案審核通過", ticketId);

        RepairTicket ticket = repairTicketRepository.findById(ticketId).orElse(null);
        if (ticket == null) return;

        // 1. 恢復設備狀態：維修中 → 正常
        if (ticket.getDeviceId() != null) {
            deviceService.updateStatus(ticket.getDeviceId(), DeviceStatus.ACTIVE);

            // 2. 寫入 device_events 歷程（含 repair_ticket_id FK）
            deviceEventService.recordEvent(
                    ticket.getDeviceId(),
                    DeviceEventType.REPAIR,
                    String.format("報修工單 %s 結案。維修描述：%s",
                            ticket.getTicketNumber(),
                            ticket.getRepairDescription() != null ? ticket.getRepairDescription() : ""),
                    null, ticketId, null);
        }
    }
}
