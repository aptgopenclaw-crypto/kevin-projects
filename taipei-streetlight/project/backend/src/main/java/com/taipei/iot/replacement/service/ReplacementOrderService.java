package com.taipei.iot.replacement.service;

import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.dept.service.DataScopeHelper;
import com.taipei.iot.device.dto.ComponentReplaceRequest;
import com.taipei.iot.device.dto.DeviceRequest;
import com.taipei.iot.device.dto.DeviceResponse;
import com.taipei.iot.device.enums.DeviceType;
import com.taipei.iot.device.service.DeviceEventService;
import com.taipei.iot.device.service.DeviceService;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.replacement.dto.ReplacementItemResponse;
import com.taipei.iot.replacement.dto.ReplacementOrderQueryParams;
import com.taipei.iot.replacement.dto.ReplacementOrderRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderResponse;
import com.taipei.iot.replacement.dto.SelfCheckItemRequest;
import com.taipei.iot.replacement.dto.SelfCheckRequest;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.service.WorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReplacementOrderService {

    private final ReplacementOrderRepository repo;
    private final ReplacementItemRepository itemRepo;
    private final RepairTicketRepository repairTicketRepo;
    private final DeviceService deviceService;
    private final DeviceEventService deviceEventService;
    private final WorkflowService workflowService;
    private final DataScopeHelper dataScopeHelper;

    private final AtomicLong dailySequence = new AtomicLong(1);
    private volatile String lastDate = "";

    // ─── 查詢 ───

    public Page<ReplacementOrderResponse> list(ReplacementOrderQueryParams params, Pageable pageable) {
        List<Long> visibleDeptIds = dataScopeHelper.getVisibleDeptIds();
        return repo.findByFilters(
                params.getStatus(),
                params.getOrderType(),
                params.getContractId(),
                params.getKeyword(),
                visibleDeptIds.isEmpty() ? null : visibleDeptIds,
                params.getDateFrom(),
                params.getDateTo(),
                pageable
        ).map(this::toResponse);
    }

    public ReplacementOrderResponse getById(Long id) {
        ReplacementOrder order = findOrThrow(id);
        ReplacementOrderResponse response = toResponse(order);
        response.setItems(itemRepo.findByOrderId(id).stream()
                .map(this::toItemResponse).toList());
        try {
            WorkflowInstance instance = workflowService.findByTicket("REPLACEMENT_ORDER", id);
            response.setCurrentStep(instance.getCurrentStep());
        } catch (Exception ignored) {
            // workflow instance may not exist yet
        }
        return response;
    }

    // ─── 建單（雙路徑） ───

    @Transactional
    @AuditEvent(AuditEventType.CREATE_REPLACEMENT_ORDER)
    public ReplacementOrderResponse createFromRepair(Long repairTicketId, ReplacementOrderRequest request) {
        RepairTicket repair = repairTicketRepo.findById(repairTicketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPAIR_TICKET_NOT_FOUND));

        ReplacementOrder order = ReplacementOrder.builder()
                .orderNumber(generateOrderNumber())
                .repairTicketId(repairTicketId)
                .contractId(repair.getContractId())
                .orderType(request.getOrderType())
                .dispatchReason(request.getDispatchReason())
                .location(request.getLocation())
                .expectedQuantity(request.getExpectedQuantity())
                .workPeriodStart(request.getWorkPeriodStart())
                .workPeriodEnd(request.getWorkPeriodEnd())
                .assignedContractor(request.getAssignedContractor())
                .status(ReplacementOrderStatus.DRAFT)
                .deptId(repair.getDeptId())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        ReplacementOrder saved = repo.save(order);
        workflowService.createInstance(
                "REPLACEMENT_REVIEW", "REPLACEMENT_ORDER", saved.getId(),
                SecurityContextUtils.getCurrentUserId());
        return toResponse(saved);
    }

    @Transactional
    @AuditEvent(AuditEventType.CREATE_REPLACEMENT_ORDER)
    public ReplacementOrderResponse createDirect(ReplacementOrderRequest request) {
        ReplacementOrder order = ReplacementOrder.builder()
                .orderNumber(generateOrderNumber())
                .orderType(request.getOrderType())
                .contractId(request.getContractId())
                .dispatchReason(request.getDispatchReason())
                .location(request.getLocation())
                .expectedQuantity(request.getExpectedQuantity())
                .workPeriodStart(request.getWorkPeriodStart())
                .workPeriodEnd(request.getWorkPeriodEnd())
                .assignedContractor(request.getAssignedContractor())
                .status(ReplacementOrderStatus.DRAFT)
                .deptId(request.getDeptId())
                .createdBy(SecurityContextUtils.getCurrentUserId())
                .build();

        ReplacementOrder saved = repo.save(order);
        workflowService.createInstance(
                "REPLACEMENT_REVIEW", "REPLACEMENT_ORDER", saved.getId(),
                SecurityContextUtils.getCurrentUserId());
        return toResponse(saved);
    }

    @Transactional
    @AuditEvent(AuditEventType.UPDATE_REPLACEMENT_ORDER)
    public ReplacementOrderResponse update(Long id, ReplacementOrderRequest request) {
        ReplacementOrder order = findOrThrow(id);
        validateStatus(order, ReplacementOrderStatus.DRAFT);

        order.setOrderType(request.getOrderType());
        order.setContractId(request.getContractId());
        order.setDispatchReason(request.getDispatchReason());
        order.setLocation(request.getLocation());
        order.setExpectedQuantity(request.getExpectedQuantity());
        order.setWorkPeriodStart(request.getWorkPeriodStart());
        order.setWorkPeriodEnd(request.getWorkPeriodEnd());
        order.setAssignedContractor(request.getAssignedContractor());
        order.setDeptId(request.getDeptId());
        return toResponse(repo.save(order));
    }

    // ─── 狀態轉換 ───

    @Transactional
    @AuditEvent(AuditEventType.UPDATE_REPLACEMENT_ORDER)
    public void dispatch(Long orderId, ReplacementOrderRequest request) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.DRAFT);

        order.setAssignedContractor(request.getAssignedContractor());
        order.setWorkPeriodStart(request.getWorkPeriodStart());
        order.setWorkPeriodEnd(request.getWorkPeriodEnd());
        order.setStatus(ReplacementOrderStatus.DISPATCHED);
        repo.save(order);

        transitionWorkflow(orderId, "DISPATCHED", "DISPATCH", "派工至承商");
    }

    @Transactional
    public void startWork(Long orderId) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.DISPATCHED);

        order.setStatus(ReplacementOrderStatus.IN_PROGRESS);
        repo.save(order);

        transitionWorkflow(orderId, "IN_PROGRESS", "START_WORK", "承商開工");
    }

    @Transactional
    @AuditEvent(AuditEventType.SELF_CHECK_REPLACEMENT)
    public void selfCheck(Long orderId, SelfCheckRequest request) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.IN_PROGRESS);

        for (var itemCheck : request.getItems()) {
            ReplacementItem item = itemRepo.findById(itemCheck.getItemId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));

            ComponentReplaceRequest replaceReq = ComponentReplaceRequest.builder()
                    .oldDeviceId(item.getOldDeviceId())
                    .newDevice(buildNewDeviceRequest(item, itemCheck))
                    .reason("換裝派工單 " + order.getOrderNumber())
                    .build();

            DeviceResponse newDevice = deviceService.replaceComponent(
                    item.getParentDeviceId(), replaceReq, deviceEventService);

            item.setNewDeviceId(newDevice.getId());
            item.setAfterSpec(newDevice.getAttributes());
            item.setStatus(ReplacementItemStatus.COMPLETED);
            item.setCompletedAt(LocalDateTime.now());
            item.setCompletedBy(SecurityContextUtils.getCurrentUsername());
            item.setNotes(itemCheck.getNotes());
            itemRepo.save(item);
        }

        order.setStatus(ReplacementOrderStatus.SELF_CHECKED);
        repo.save(order);

        transitionWorkflow(orderId, "SELF_CHECKED", "SELF_CHECK", "廠商自主檢核完成");
    }

    @Transactional
    public void submitReview(Long orderId) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.SELF_CHECKED);

        order.setStatus(ReplacementOrderStatus.PENDING_REVIEW);
        repo.save(order);

        transitionWorkflow(orderId, "PENDING_REVIEW", "SUBMIT_REVIEW", "報竣送審");
    }

    @Transactional
    @AuditEvent(AuditEventType.CLOSE_REPLACEMENT)
    public void approve(Long orderId, String comment) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.PENDING_REVIEW);

        transitionWorkflow(orderId, "CLOSED", "APPROVE", comment);
    }

    @Transactional
    public void returnOrder(Long orderId, String comment) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.PENDING_REVIEW);

        order.setStatus(ReplacementOrderStatus.RETURNED);
        repo.save(order);

        transitionWorkflow(orderId, "RETURNED", "RETURN", comment);
    }

    @Transactional
    public void resubmit(Long orderId) {
        ReplacementOrder order = findOrThrow(orderId);
        validateStatus(order, ReplacementOrderStatus.RETURNED);

        order.setStatus(ReplacementOrderStatus.PENDING_REVIEW);
        repo.save(order);

        transitionWorkflow(orderId, "PENDING_REVIEW", "RESUBMIT", "補件重送");
    }

    // ─── 私有輔助方法 ───

    ReplacementOrder findOrThrow(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REPLACEMENT_ORDER_NOT_FOUND));
    }

    private void validateStatus(ReplacementOrder order, ReplacementOrderStatus... expected) {
        if (!Set.of(expected).contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.REPLACEMENT_INVALID_STATUS);
        }
    }

    private void transitionWorkflow(Long orderId, String targetStep, String action, String comment) {
        WorkflowInstance instance = workflowService.findByTicket("REPLACEMENT_ORDER", orderId);
        workflowService.transition(
                instance.getId(),
                targetStep,
                action,
                SecurityContextUtils.getCurrentUserId(),
                SecurityContextUtils.getCurrentUsername(),
                comment,
                null);
    }

    private DeviceRequest buildNewDeviceRequest(ReplacementItem item, SelfCheckItemRequest itemCheck) {
        return DeviceRequest.builder()
                .deviceType(DeviceType.valueOf(
                        item.getAfterDeviceType() != null ? item.getAfterDeviceType() : item.getBeforeDeviceType()))
                .deviceCode(itemCheck.getDeviceCode())
                .parentDeviceId(item.getParentDeviceId())
                .attributes(item.getAfterSpec())
                .build();
    }

    private synchronized String generateOrderNumber() {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(lastDate)) {
            lastDate = today;
            dailySequence.set(1);
        }
        return String.format("RO-%s-%03d", today, dailySequence.getAndIncrement());
    }

    private ReplacementOrderResponse toResponse(ReplacementOrder o) {
        return ReplacementOrderResponse.builder()
                .id(o.getId())
                .orderNumber(o.getOrderNumber())
                .repairTicketId(o.getRepairTicketId())
                .contractId(o.getContractId())
                .orderType(o.getOrderType())
                .dispatchReason(o.getDispatchReason())
                .location(o.getLocation())
                .expectedQuantity(o.getExpectedQuantity())
                .workPeriodStart(o.getWorkPeriodStart())
                .workPeriodEnd(o.getWorkPeriodEnd())
                .assignedContractor(o.getAssignedContractor())
                .status(o.getStatus())
                .deptId(o.getDeptId())
                .createdBy(o.getCreatedBy())
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private ReplacementItemResponse toItemResponse(ReplacementItem item) {
        return ReplacementItemResponse.builder()
                .id(item.getId())
                .orderId(item.getOrderId())
                .parentDeviceId(item.getParentDeviceId())
                .oldDeviceId(item.getOldDeviceId())
                .newDeviceId(item.getNewDeviceId())
                .beforeDeviceType(item.getBeforeDeviceType())
                .beforeSpec(item.getBeforeSpec())
                .afterDeviceType(item.getAfterDeviceType())
                .afterSpec(item.getAfterSpec())
                .materialSpecId(item.getMaterialSpecId())
                .approvedMaterialId(item.getApprovedMaterialId())
                .status(item.getStatus())
                .completedAt(item.getCompletedAt())
                .completedBy(item.getCompletedBy())
                .notes(item.getNotes())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
