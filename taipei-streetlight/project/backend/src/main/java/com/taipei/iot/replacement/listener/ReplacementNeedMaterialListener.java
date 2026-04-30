package com.taipei.iot.replacement.listener;

import com.taipei.iot.material.service.IssueService;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.workflow.event.WorkflowTransitionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * E6：換裝派工時自動產生領料需求
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReplacementNeedMaterialListener {

    private final ReplacementOrderRepository orderRepo;
    private final IssueService issueService;

    @EventListener
    @Transactional
    public void onReplacementDispatched(WorkflowTransitionEvent event) {
        if (!"REPLACEMENT_REVIEW".equals(event.getInstance().getWorkflowType())) return;
        if (!"DISPATCHED".equals(event.getTargetStep())) return;

        Long orderId = event.getInstance().getTicketId();
        log.info("E6 事件：換裝派工單 {} 已派工，建立領料需求", orderId);

        ReplacementOrder order = orderRepo.findById(orderId).orElse(null);
        if (order == null) return;

        issueService.createFromReplacement(orderId);
        log.info("已為換裝派工單 {} 建立領料需求", order.getOrderNumber());
    }
}
