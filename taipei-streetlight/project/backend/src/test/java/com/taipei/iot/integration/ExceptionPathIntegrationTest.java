package com.taipei.iot.integration;

import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.repository.DeviceEventRepository;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.fault.service.FaultTicketService;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.IssueRequest;
import com.taipei.iot.material.dto.IssueRecordRequest;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.material.repository.IssueRecordRepository;
import com.taipei.iot.material.repository.IssueRequestRepository;
import com.taipei.iot.material.service.IssueService;
import com.taipei.iot.repair.dto.CompletionReportRequest;
import com.taipei.iot.repair.dto.DispatchRequest;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.repository.RepairDispatchRepository;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.repair.service.RepairDispatchService;
import com.taipei.iot.repair.service.RepairTicketService;
import com.taipei.iot.replacement.dto.ReplacementOrderRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderResponse;
import com.taipei.iot.replacement.entity.ReplacementItem;
import com.taipei.iot.replacement.entity.ReplacementOrder;
import com.taipei.iot.replacement.enums.ReplacementItemStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import com.taipei.iot.replacement.repository.ReplacementItemRepository;
import com.taipei.iot.replacement.repository.ReplacementOrderRepository;
import com.taipei.iot.replacement.service.ReplacementOrderService;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import com.taipei.iot.workflow.repository.WorkflowInstanceRepository;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import com.taipei.iot.workflow.service.WorkflowService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.taipei.iot.integration.IntegrationTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 異常路徑整合測試 — 10 scenarios
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class ExceptionPathIntegrationTest {

    @Autowired private FaultTicketService faultTicketService;
    @Autowired private RepairTicketService repairTicketService;
    @Autowired private RepairDispatchService repairDispatchService;
    @Autowired private ReplacementOrderService replacementOrderService;
    @Autowired private IssueService issueService;
    @Autowired private WorkflowService workflowService;

    @Autowired private FaultTicketRepository faultTicketRepo;
    @Autowired private RepairTicketRepository repairTicketRepo;
    @Autowired private RepairDispatchRepository repairDispatchRepo;
    @Autowired private ReplacementOrderRepository orderRepo;
    @Autowired private ReplacementItemRepository itemRepo;
    @Autowired private IssueRequestRepository issueRequestRepo;
    @Autowired private IssueRecordRepository issueRecordRepo;
    @Autowired private InventoryRepository inventoryRepo;
    @Autowired private DeviceRepository deviceRepo;
    @Autowired private DeviceEventRepository deviceEventRepo;
    @Autowired private WorkflowInstanceRepository workflowInstanceRepo;
    @Autowired private WorkflowStepLogRepository stepLogRepo;
    @Autowired private DeptInfoRepository deptInfoRepo;

    private Long squad1DeptId;
    private Long engDeptId;
    private Long admDeptId;
    private Long deviceId;      // LUM-XINYI-001
    private Long device3Id;     // LUM-XINYI-003
    private Long poleId;
    private Long pole2Id;
    private Long oldLumId;      // LUM-XINYI-002
    private Long contractId;
    private Long materialSpecId;
    private Long inventoryId;
    private Integer inventoryBefore;

    // cleanup tracking
    private final List<Long> workflowIds = new ArrayList<>();
    private final List<Long> repairTicketIds = new ArrayList<>();
    private final List<Long> faultTicketIds = new ArrayList<>();
    private final List<Long> orderIds = new ArrayList<>();
    private final List<Long> issueRequestIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(TENANT_A);
        lookupSeedIds();
    }

    @AfterEach
    void tearDown() {
        try {
            TenantContext.setCurrentTenantId(TENANT_A);
            loginAs("u-tpe-admin", 1L, "ALL",
                    List.of("FAULT_MANAGE", "REPAIR_MANAGE", "REPLACEMENT_MANAGE",
                            "MATERIAL_MANAGE", "DEVICE_MANAGE", "WORKFLOW_MANAGE"));

            // Cleanup in reverse dependency order
            for (Long id : issueRequestIds) {
                issueRecordRepo.deleteByRequestId(id);
                issueRequestRepo.deleteById(id);
            }
            for (Long id : orderIds) {
                List<ReplacementItem> items = itemRepo.findByOrderId(id);
                List<Long> itemIds = items.stream().map(ReplacementItem::getId).toList();
                if (!itemIds.isEmpty()) deviceEventRepo.deleteByReplacementItemIdIn(itemIds);
                items.forEach(item -> {
                    if (item.getNewDeviceId() != null) {
                        deviceEventRepo.findByDeviceId(item.getNewDeviceId()).forEach(deviceEventRepo::delete);
                        deviceRepo.deleteById(item.getNewDeviceId());
                    }
                });
                items.forEach(item -> itemRepo.deleteById(item.getId()));
                orderRepo.deleteById(id);
            }
            for (Long id : repairTicketIds) {
                deviceEventRepo.deleteByRepairTicketId(id);
                repairDispatchRepo.deleteByRepairTicketId(id);
                repairTicketRepo.deleteById(id);
            }
            for (Long id : faultTicketIds) {
                faultTicketRepo.deleteById(id);
            }
            for (Long id : workflowIds) {
                stepLogRepo.deleteByInstanceId(id);
                workflowInstanceRepo.deleteById(id);
            }

            // Reset device statuses
            resetDevice(deviceId);
            resetDevice(device3Id);
            resetDevice(oldLumId);

            // Reset inventory
            if (inventoryId != null && inventoryBefore != null) {
                inventoryRepo.findById(inventoryId).ifPresent(inv -> {
                    inv.setQuantityOnHand(inventoryBefore);
                    inventoryRepo.save(inv);
                });
            }
        } finally {
            IntegrationTestHelper.logout();
        }
    }

    // ═══ #1 退回重做：結案駁回 → RETURNED → 重新施工 ═══
    @Test
    void scenario1_repairReturnedAndRedo() {
        // 簡化：建報修 → 走到 COMPLETION_REPORTED → 建 REPAIR_CLOSE → 駁回 → 重做
        loginAsSquad1Op(squad1DeptId);
        FaultTicketResponse fault = createFaultAndConfirm(deviceId);
        RepairTicket repair = findRepairByFaultId(fault.getId());
        repairTicketIds.add(repair.getId());

        // accept + dispatch + in_progress + complete
        repairTicketService.accept(repair.getId());
        dispatchRepair(repair.getId());

        loginAsSquad1Field(squad1DeptId);
        WorkflowInstance dispatchWf = workflowService.findByTicket("REPAIR_TICKET", repair.getId());
        workflowService.transition(dispatchWf.getId(), "IN_PROGRESS", "開工",
                "u-squad1-field", "蔡文傑", "到場施工", null);
        repairTicketService.reportCompletion(repair.getId(),
                CompletionReportRequest.builder().repairDescription("維修完成").faultCause("鬆脫").build());

        // 建 REPAIR_CLOSE → 駁回
        loginAsSquad1Mgr(squad1DeptId);
        Long closeWfId = workflowService.createInstance("REPAIR_CLOSE", "REPAIR_TICKET",
                repair.getId(), "u-squad1-field");
        workflowIds.add(closeWfId);

        workflowService.transition(closeWfId, "PENDING_REVIEW", "送審",
                "u-squad1-mgr", "李明華", "審查中", null);

        // 駁回 → RETURNED
        workflowService.transition(closeWfId, "RETURNED", "退回",
                "u-squad1-mgr", "李明華", "照片不清楚，請重新拍攝", null);

        WorkflowInstance returnedWf = workflowInstanceRepo.findById(closeWfId).orElseThrow();
        assertEquals("RETURNED", returnedWf.getCurrentStep());

        // 重新完工回報 → COMPLETION_REPORTED
        loginAsSquad1Field(squad1DeptId);
        workflowService.transition(closeWfId, "COMPLETION_REPORTED", "重新回報",
                "u-squad1-field", "蔡文傑", "已重新拍攝清晰照片", null);

        returnedWf = workflowInstanceRepo.findById(closeWfId).orElseThrow();
        assertEquals("COMPLETION_REPORTED", returnedWf.getCurrentStep());
    }

    // ═══ #5 領料不足：庫存 < 需求量 ═══
    @Test
    void scenario5_insufficientInventory() {
        loginAsEngOff1(engDeptId);

        ReplacementOrderResponse orderResp = createReplacementOrder();
        orderIds.add(orderResp.getId());
        WorkflowInstance wf = workflowService.findByTicket("REPLACEMENT_ORDER", orderResp.getId());
        workflowIds.add(wf.getId());

        // 派工 → 建領料
        loginAsSquad1Op(squad1DeptId);
        replacementOrderService.dispatch(orderResp.getId(), ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT)
                .assignedContractor("設備商")
                .workPeriodStart(LocalDate.now())
                .workPeriodEnd(LocalDate.now().plusDays(7))
                .build());

        List<IssueRequest> issueReqs = issueRequestRepo.findByReplacementOrderId(orderResp.getId());
        assertFalse(issueReqs.isEmpty());
        Long issueReqId = issueReqs.get(0).getId();
        issueRequestIds.add(issueReqId);

        // 審核通過
        loginAsAdmWarehouse(admDeptId);
        issueService.approve(issueReqId);

        // 嘗試領超過庫存量的數量
        IssueRecordRequest bigRequest = IssueRecordRequest.builder()
                .inventoryId(inventoryId)
                .materialSpecId(materialSpecId)
                .quantity(99999)
                .build();

        assertThrows(BusinessException.class, () ->
                        issueService.issue(issueReqId, List.of(bigRequest)),
                "庫存不足應拋出 BusinessException");

        // 驗證庫存不變
        Inventory inv = inventoryRepo.findById(inventoryId).orElseThrow();
        assertEquals(inventoryBefore, inv.getQuantityOnHand(), "庫存應未變動");
    }

    // ═══ #8 換裝結案駁回 → RETURNED → 回到自檢步驟 ═══
    @Test
    void scenario8_replacementReturnedAndResubmit() {
        loginAsEngOff1(engDeptId);

        ReplacementOrderResponse orderResp = createReplacementOrder();
        orderIds.add(orderResp.getId());
        WorkflowInstance wf = workflowService.findByTicket("REPLACEMENT_ORDER", orderResp.getId());
        workflowIds.add(wf.getId());

        // 走到 PENDING_REVIEW
        loginAsSquad1Op(squad1DeptId);
        replacementOrderService.dispatch(orderResp.getId(), ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT)
                .assignedContractor("設備商")
                .workPeriodStart(LocalDate.now())
                .workPeriodEnd(LocalDate.now().plusDays(7))
                .build());

        List<IssueRequest> issueReqs = issueRequestRepo.findByReplacementOrderId(orderResp.getId());
        if (!issueReqs.isEmpty()) issueRequestIds.add(issueReqs.get(0).getId());

        loginAsVendorField1(13L);
        replacementOrderService.startWork(orderResp.getId());

        // 跳過 selfCheck（需要 item），直接透過 workflow transition 模擬到 SELF_CHECKED
        // 因為 selfCheck 需要 replaceComponent，過於複雜，改用 workflow 直接推進
        wf = workflowInstanceRepo.findById(wf.getId()).orElseThrow();
        assertEquals("IN_PROGRESS", wf.getCurrentStep());

        // 手動推進 workflow 到 SELF_CHECKED + PENDING_REVIEW
        loginAsVendorField1(13L);
        // IN_PROGRESS → SELF_CHECKED (via service)
        // 簡化：直接 update order status + transition workflow
        ReplacementOrder order = orderRepo.findById(orderResp.getId()).orElseThrow();
        order.setStatus(ReplacementOrderStatus.SELF_CHECKED);
        orderRepo.save(order);
        workflowService.transition(wf.getId(), "SELF_CHECKED", "自檢",
                "u-vendor-field1", "葉建廷", "自檢完成", null);

        // SELF_CHECKED → PENDING_REVIEW
        replacementOrderService.submitReview(orderResp.getId());

        // 駁回
        loginAsEngMgr(engDeptId);
        replacementOrderService.returnOrder(orderResp.getId(), "自檢報告缺漏，請補件");

        order = orderRepo.findById(orderResp.getId()).orElseThrow();
        assertEquals(ReplacementOrderStatus.RETURNED, order.getStatus());

        wf = workflowInstanceRepo.findById(wf.getId()).orElseThrow();
        assertEquals("RETURNED", wf.getCurrentStep());

        // 重新提交
        loginAsVendorField1(13L);
        replacementOrderService.resubmit(orderResp.getId());

        order = orderRepo.findById(orderResp.getId()).orElseThrow();
        assertEquals(ReplacementOrderStatus.PENDING_REVIEW, order.getStatus());

        wf = workflowInstanceRepo.findById(wf.getId()).orElseThrow();
        assertEquals("PENDING_REVIEW", wf.getCurrentStep());
    }

    // ═══ #9 併案結案：兩張報修單各自獨立結案 ═══
    @Test
    void scenario9_parallelRepairClosures() {
        // 建兩張障礙 → 兩張報修
        loginAsSquad1Op(squad1DeptId);
        FaultTicketResponse fault1 = createFaultAndConfirm(deviceId);
        FaultTicketResponse fault2 = createFaultAndConfirm(device3Id);

        RepairTicket repair1 = findRepairByFaultId(fault1.getId());
        RepairTicket repair2 = findRepairByFaultId(fault2.getId());
        repairTicketIds.add(repair1.getId());
        repairTicketIds.add(repair2.getId());

        // 各自走完 dispatch → in_progress → complete
        for (RepairTicket rt : List.of(repair1, repair2)) {
            loginAsSquad1Op(squad1DeptId);
            repairTicketService.accept(rt.getId());
            dispatchRepair(rt.getId());

            loginAsSquad1Field(squad1DeptId);
            WorkflowInstance dispWf = workflowService.findByTicket("REPAIR_TICKET", rt.getId());
            workflowService.transition(dispWf.getId(), "IN_PROGRESS", "開工",
                    "u-squad1-field", "蔡文傑", "施工", null);
            repairTicketService.reportCompletion(rt.getId(),
                    CompletionReportRequest.builder().repairDescription("修復完成").faultCause("故障").build());
        }

        // 各自建 REPAIR_CLOSE → 結案
        for (RepairTicket rt : List.of(repair1, repair2)) {
            loginAsSquad1Mgr(squad1DeptId);
            Long closeWfId = workflowService.createInstance("REPAIR_CLOSE", "REPAIR_TICKET",
                    rt.getId(), "u-squad1-field");
            workflowIds.add(closeWfId);
            workflowService.transition(closeWfId, "PENDING_REVIEW", "送審",
                    "u-squad1-mgr", "李明華", "審查", null);
            workflowService.transition(closeWfId, "CLOSED", "結案",
                    "u-squad1-mgr", "李明華", "結案", null);
        }

        // 驗證兩台設備各自恢復 ACTIVE
        Device dev1 = deviceRepo.findById(deviceId).orElseThrow();
        Device dev3 = deviceRepo.findById(device3Id).orElseThrow();
        assertEquals(DeviceStatus.ACTIVE, dev1.getStatus());
        assertEquals(DeviceStatus.ACTIVE, dev3.getStatus());
    }

    // ── 共用輔助方法 ──

    private FaultTicketResponse createFaultAndConfirm(Long devId) {
        loginAsSquad1Off1(squad1DeptId);
        FaultTicketResponse fault = faultTicketService.create(FaultTicketRequest.builder()
                .deviceId(devId)
                .source(FaultTicketSource.CITIZEN_REPORT)
                .priority("NORMAL")
                .description("燈桿不亮")
                .build());
        faultTicketIds.add(fault.getId());

        loginAsSquad1Op(squad1DeptId);
        Long faultWfId = workflowService.createInstance("FAULT_REVIEW", "FAULT_TICKET",
                fault.getId(), "u-squad1-off1");
        workflowIds.add(faultWfId);
        workflowService.transition(faultWfId, "REVIEW", "送審",
                "u-squad1-op", "周志豪", "審查", null);
        workflowService.transition(faultWfId, "CONFIRMED", "確認",
                "u-squad1-op", "周志豪", "確認為障礙", null);
        return fault;
    }

    private RepairTicket findRepairByFaultId(Long faultTicketId) {
        return repairTicketRepo.findAll().stream()
                .filter(rt -> faultTicketId.equals(rt.getFaultTicketId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Repair ticket not found for fault " + faultTicketId));
    }

    private void dispatchRepair(Long repairTicketId) {
        repairDispatchService.dispatch(repairTicketId, DispatchRequest.builder()
                .assignedOrg("蔡文傑")
                .contractId(contractId)
                .dueDate(LocalDate.now().plusDays(3))
                .build());
    }

    private ReplacementOrderResponse createReplacementOrder() {
        return replacementOrderService.createDirect(ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT)
                .contractId(contractId)
                .dispatchReason("燈具更換")
                .location("信義路三段")
                .expectedQuantity(1)
                .workPeriodStart(LocalDate.now())
                .workPeriodEnd(LocalDate.now().plusDays(14))
                .assignedContractor("設備商")
                .deptId(engDeptId)
                .build());
    }

    private void resetDevice(Long devId) {
        if (devId == null) return;
        deviceRepo.findById(devId).ifPresent(d -> {
            d.setStatus(DeviceStatus.ACTIVE);
            d.setDecommissionedAt(null);
            deviceRepo.save(d);
        });
    }

    private void lookupSeedIds() {
        loginAs("u-tpe-admin", 1L, "ALL",
                List.of("DEVICE_VIEW", "MATERIAL_VIEW", "REPLACEMENT_VIEW", "WORKFLOW_VIEW",
                        "FAULT_VIEW", "REPAIR_VIEW"));

        Device lum001 = deviceRepo.findByTenantIdAndDeviceCode(TENANT_A, "LUM-XINYI-001")
                .orElseThrow();
        deviceId = lum001.getId();
        poleId = lum001.getParentDeviceId();
        squad1DeptId = lum001.getDeptId();
        contractId = lum001.getContractId();

        Device lum003 = deviceRepo.findByTenantIdAndDeviceCode(TENANT_A, "LUM-XINYI-003")
                .orElseThrow();
        device3Id = lum003.getId();

        Device lum002 = deviceRepo.findByTenantIdAndDeviceCode(TENANT_A, "LUM-XINYI-002")
                .orElseThrow();
        oldLumId = lum002.getId();
        pole2Id = lum002.getParentDeviceId();

        engDeptId = deptInfoRepo.findByTenantIdAndDeptName(TENANT_A, "工程股")
                .orElseThrow().getDeptId();
        admDeptId = deptInfoRepo.findByTenantIdAndDeptName(TENANT_A, "行政股")
                .orElseThrow().getDeptId();

        List<Inventory> allInv = inventoryRepo.findAll();
        Inventory led150 = allInv.stream()
                .filter(inv -> inv.getMaterialSpec() != null
                        && "MS-LED-150W".equals(inv.getMaterialSpec().getSpecCode()))
                .findFirst().orElseThrow();
        inventoryId = led150.getId();
        materialSpecId = led150.getMaterialSpecId();
        inventoryBefore = led150.getQuantityOnHand();
    }
}
