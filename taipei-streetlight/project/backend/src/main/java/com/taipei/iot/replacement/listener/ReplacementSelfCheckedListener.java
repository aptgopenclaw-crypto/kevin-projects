package com.taipei.iot.replacement.listener;

import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
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
 * E11：廠商自主檢核通過 → 設備預更新（provisional），寫入歷程
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplacementSelfCheckedListener {

    private final ReplacementOrderRepository orderRepo;
    private final ReplacementItemRepository itemRepo;
    private final DeviceEventService deviceEventService;

    @EventListener
    @Transactional
    public void onSelfChecked(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"SELF_CHECKED".equals(event.getTargetStep())) return;

        Long orderId = event.getInstance().getTicketId();
        log.info("E11 事件：換裝派工單 {} 廠商自主檢核通過", orderId);

        ReplacementOrder order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        List<ReplacementItem> items = itemRepo.findByOrderId(orderId);
        for (ReplacementItem item : items) {
            if (item.getNewDeviceId() != null) {
                deviceEventService.recordEvent(
                        item.getNewDeviceId(),
                        DeviceEventType.INSPECT,
                        String.format("換裝單 %s 廠商自主檢核通過（待正式驗收）", order.getOrderNumber()),
                        null, null, item.getId());
            }
        }

        log.info("換裝派工單 {} 自主檢核完成，處理 {} 項明細", order.getOrderNumber(), items.size());
    }
}
