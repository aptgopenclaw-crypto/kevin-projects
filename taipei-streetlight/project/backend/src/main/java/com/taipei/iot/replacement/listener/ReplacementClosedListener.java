package com.taipei.iot.replacement.listener;

import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * E10：換裝結案審核通過 → 更新訂單狀態 CLOSED + 舊設備下線 + 新設備上線 + 寫入設備歷程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplacementClosedListener {

    private final ReplacementOrderRepository orderRepo;
    private final ReplacementItemRepository itemRepo;
    private final DeviceEventService deviceEventService;
    private final DeviceService deviceService;

    @EventListener
    @Transactional
    public void onReplacementClosed(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"CLOSED".equals(event.getTargetStep())) return;

        Long orderId = event.getInstance().getTicketId();
        log.info("E10 事件：換裝派工單 {} 結案審核通過", orderId);

        ReplacementOrder order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        order.setStatus(ReplacementOrderStatus.CLOSED);
        orderRepo.save(order);

        // 遍歷換裝明細，更新設備狀態 + 寫入歷程
        List<ReplacementItem> items = itemRepo.findByOrderId(orderId);
        for (ReplacementItem item : items) {
            // 舊設備 → 除役
            if (item.getOldDeviceId() != null) {
                deviceService.updateStatus(item.getOldDeviceId(), DeviceStatus.DECOMMISSIONED);
                deviceEventService.recordEvent(
                        item.getOldDeviceId(),
                        DeviceEventType.DECOMMISSION,
                        String.format("換裝單 %s 結案，設備除役", order.getOrderNumber()),
                        null, null, item.getId());
            }

            // 新設備 → 上線
            if (item.getNewDeviceId() != null) {
                deviceService.updateStatus(item.getNewDeviceId(), DeviceStatus.ACTIVE);
                deviceEventService.recordEvent(
                        item.getNewDeviceId(),
                        DeviceEventType.REPLACE,
                        String.format("換裝單 %s 結案，新設備上線（替換設備 ID %d）",
                                order.getOrderNumber(),
                                item.getOldDeviceId() != null ? item.getOldDeviceId() : 0),
                        null, null, item.getId());
            }
        }

        log.info("換裝派工單 {} 已結案，處理 {} 項明細", order.getOrderNumber(), items.size());
    }
}
