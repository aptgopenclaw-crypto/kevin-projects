package com.taipei.iot.integration;

import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.entity.DeviceEvent;
import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.repository.DeviceEventRepository;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.material.entity.Inventory;
import com.taipei.iot.material.entity.IssueRequest;
import com.taipei.iot.material.enums.IssueRequestStatus;
import com.taipei.iot.material.repository.InventoryRepository;
import com.taipei.iot.material.repository.IssueRecordRepository;
import com.taipei.iot.material.repository.IssueRequestRepository;
import com.taipei.iot.material.service.IssueService;
import com.taipei.iot.material.dto.IssueRecordRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderResponse;
import com.taipei.iot.replacement.dto.SelfCheckItemRequest;
import com.taipei.iot.replacement.dto.SelfCheckRequest;
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
import com.taipei.iot.workflow.enums.WorkflowStatus;
import com.taipei.iot.workflow.repository.WorkflowInstanceRepository;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import com.taipei.iot.workflow.service.WorkflowService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static com.taipei.iot.integration.IntegrationTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Happy Path 2：換裝領料閉環 — 信義路三段 #002 燈桿需換裝新設備
 * <p>
 * 流程：建立換裝申請 → 派工(E6 自動建領料) → 領料出庫(E7) → 施工 → 自檢(E11) → 驗收 → 結案(E10)
 * <p>
 * 事件鏈：E6→E7→E11→E10
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ReplacementHappyPathIntegrationTest {

    @Autowired private ReplacementOrderService replacementOrderService;
    @Autowired private IssueService issueService;
    @Autowired private WorkflowService workflowService;

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

    // Seed 查詢
    private Long engDeptId;
    private Long squad1DeptId;
    private Long admDeptId;
    private Long vendorDeptId;
    private Long poleId;        // POLE-XINYI-002
    private Long oldLumId;      // LUM-XINYI-002 (舊燈具)
    private Long contractId;    // CT-2026-VENDOR-001
    private Long materialSpecId; // MS-LED-150W
    private Long inventoryId;   // WH-NORTH + MS-LED-150W
    private Integer inventoryBefore;

    // 測試中產生的 ID
    private Long orderId;
    private Long replacementWfId;
    private Long issueRequestId;
    private Long newDeviceId;

    @BeforeEach
    void setUp() {
        TenantContext.setCurrentTenantId(TENANT_A);
        lookupSeedIds();
    }

    @AfterEach
    void tearDown() {
        try {
            TenantContext.setCurrentTenantId(TENANT_A);
            loginAs("u-tpe-admin", 1L, "ALL", List.of("DEVICE_MANAGE", "MATERIAL_MANAGE",
                    "REPLACEMENT_MANAGE", "WORKFLOW_MANAGE"));

            // 清理: device_events for new device + old device
            if (orderId != null) {
                List<ReplacementItem> items = itemRepo.findByOrderId(orderId);
                List<Long> itemIds = items.stream().map(ReplacementItem::getId).toList();
                if (!itemIds.isEmpty()) {
                    deviceEventRepo.deleteByReplacementItemIdIn(itemIds);
                }
                // 刪除新設備產生的 device_events
                for (ReplacementItem item : items) {
                    if (item.getNewDeviceId() != null) {
                        List<DeviceEvent> newDevEvents = deviceEventRepo.findByDeviceId(item.getNewDeviceId());
                        deviceEventRepo.deleteAll(newDevEvents);
                        // 刪除新設備
                        deviceRepo.deleteById(item.getNewDeviceId());
                    }
                }
                // 刪除 POLE 上的 REPLACE 事件
                List<DeviceEvent> poleEvents = deviceEventRepo.findByDeviceId(poleId);
                poleEvents.stream()
                        .filter(e -> e.getEventType() == DeviceEventType.REPLACE
                                || e.getEventType() == DeviceEventType.DECOMMISSION)
                        .forEach(deviceEventRepo::delete);
            }

            // 清理 issue records + requests
            if (issueRequestId != null) {
                issueRecordRepo.deleteByRequestId(issueRequestId);
                issueRequestRepo.deleteById(issueRequestId);
            }

            // 清理 replacement items + order
            if (orderId != null) {
                itemRepo.findByOrderId(orderId).forEach(item -> itemRepo.deleteById(item.getId()));
                orderRepo.deleteById(orderId);
            }

            // 清理 workflow
            if (replacementWfId != null) {
                stepLogRepo.deleteByInstanceId(replacementWfId);
                workflowInstanceRepo.deleteById(replacementWfId);
            }

            // 恢復舊設備狀態
            if (oldLumId != null) {
                deviceRepo.findById(oldLumId).ifPresent(d -> {
                    d.setStatus(DeviceStatus.ACTIVE);
                    d.setDecommissionedAt(null);
                    deviceRepo.save(d);
                });
            }

            // 恢復庫存
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

    @Test
    @Order(1)
    void happyPath2_replacementClosedLoop() {
        // ═══ Step 1: 吳佳穎（工程股）提出換裝申請 ═══
        loginAsEngOff1(engDeptId);

        ReplacementOrderRequest orderReq = ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT)
                .contractId(contractId)
                .dispatchReason("信義路 #002 燈具老舊，需更換為 150W LED")
                .location("信義路三段")
                .expectedQuantity(1)
                .workPeriodStart(LocalDate.now())
                .workPeriodEnd(LocalDate.now().plusDays(14))
                .assignedContractor("設備商")
                .deptId(engDeptId)
                .build();
        ReplacementOrderResponse orderResp = replacementOrderService.createDirect(orderReq);
        orderId = orderResp.getId();

        assertNotNull(orderId);
        assertEquals(ReplacementOrderStatus.DRAFT, orderResp.getStatus());

        // 建立 replacement_item（手動，代表需要換的燈具）
        ReplacementItem item = ReplacementItem.builder()
                .orderId(orderId)
                .parentDeviceId(poleId)
                .oldDeviceId(oldLumId)
                .beforeDeviceType("LUMINAIRE")
                .afterDeviceType("LUMINAIRE")
                .materialSpecId(materialSpecId)
                .status(ReplacementItemStatus.PENDING)
                .build();
        item = itemRepo.save(item);
        Long itemId = item.getId();

        // 取得 REPLACEMENT_REVIEW workflow
        WorkflowInstance wf = workflowService.findByTicket("REPLACEMENT_ORDER", orderId);
        replacementWfId = wf.getId();
        assertEquals("DRAFT", wf.getCurrentStep());

        // ═══ Step 2: 周志豪（維運）派工 → E6 自動建領料需求 ═══
        loginAsSquad1Op(squad1DeptId);

        ReplacementOrderRequest dispatchReq = ReplacementOrderRequest.builder()
                .orderType(ReplacementOrderType.REPLACEMENT)
                .assignedContractor("設備商")
                .workPeriodStart(LocalDate.now())
                .workPeriodEnd(LocalDate.now().plusDays(14))
                .build();
        replacementOrderService.dispatch(orderId, dispatchReq);

        // 驗證 E6：自動建立領料需求
        List<IssueRequest> issueRequests = issueRequestRepo.findByReplacementOrderId(orderId);
        assertEquals(1, issueRequests.size(), "E6 應自動建立 1 筆領料需求");
        IssueRequest issueReq = issueRequests.get(0);
        issueRequestId = issueReq.getId();
        assertEquals(IssueRequestStatus.PENDING, issueReq.getStatus());
        assertEquals(orderId, issueReq.getReplacementOrderId());

        // ═══ Step 3: 林淑芬（倉管）審核領料 → 出庫 → E7 庫存扣減 ═══
        loginAsAdmWarehouse(admDeptId);

        issueService.approve(issueRequestId);

        IssueRecordRequest issueRecordReq = IssueRecordRequest.builder()
                .inventoryId(inventoryId)
                .materialSpecId(materialSpecId)
                .quantity(1)
                .build();
        issueService.issue(issueRequestId, List.of(issueRecordReq));

        // 驗證 E7：庫存扣減
        Inventory inv = inventoryRepo.findById(inventoryId).orElseThrow();
        assertEquals(inventoryBefore - 1, inv.getQuantityOnHand(), "E7 庫存應扣減 1");

        // ═══ Step 4: 葉建廷（設備商外勤）開工 ═══
        loginAsVendorField1(vendorDeptId);

        replacementOrderService.startWork(orderId);

        ReplacementOrder order = orderRepo.findById(orderId).orElseThrow();
        assertEquals(ReplacementOrderStatus.IN_PROGRESS, order.getStatus());

        // ═══ Step 5: 葉建廷 自主檢核 → E11 設備歷程 ═══
        SelfCheckItemRequest selfCheckItem = SelfCheckItemRequest.builder()
                .itemId(itemId)
                .deviceCode("LUM-XINYI-002-NEW")
                .notes("新 150W LED 燈具安裝完成，功能正常")
                .build();
        SelfCheckRequest selfCheckReq = SelfCheckRequest.builder()
                .items(List.of(selfCheckItem))
                .build();
        replacementOrderService.selfCheck(orderId, selfCheckReq);

        // 驗證 E11：設備歷程 INSPECT
        ReplacementItem updatedItem = itemRepo.findById(itemId).orElseThrow();
        assertNotNull(updatedItem.getNewDeviceId(), "自檢後應有新設備 ID");
        newDeviceId = updatedItem.getNewDeviceId();
        assertEquals(ReplacementItemStatus.COMPLETED, updatedItem.getStatus());

        List<DeviceEvent> newDeviceEvents = deviceEventRepo.findByDeviceId(newDeviceId);
        boolean hasInspectEvent = newDeviceEvents.stream()
                .anyMatch(e -> e.getEventType() == DeviceEventType.INSPECT);
        assertTrue(hasInspectEvent, "E11 應寫入 INSPECT 事件");

        order = orderRepo.findById(orderId).orElseThrow();
        assertEquals(ReplacementOrderStatus.SELF_CHECKED, order.getStatus());

        // ═══ Step 6: 謝明達（監造）驗收確認 — 透過 workflow transition ═══
        loginAsEngMonitor(engDeptId);

        replacementOrderService.submitReview(orderId);

        order = orderRepo.findById(orderId).orElseThrow();
        assertEquals(ReplacementOrderStatus.PENDING_REVIEW, order.getStatus());

        // ═══ Step 7: 黃建中（工程股長）結案審核 → E10 新舊設備更新 ═══
        loginAsEngMgr(engDeptId);

        replacementOrderService.approve(orderId, "驗收合格，核准結案");

        // 驗證 E10：舊設備 → DECOMMISSIONED
        Device oldDevice = deviceRepo.findById(oldLumId).orElseThrow();
        assertEquals(DeviceStatus.DECOMMISSIONED, oldDevice.getStatus(),
                "E10 舊設備應除役");

        // 驗證 E10：新設備 → ACTIVE
        Device newDevice = deviceRepo.findById(newDeviceId).orElseThrow();
        assertEquals(DeviceStatus.ACTIVE, newDevice.getStatus(),
                "E10 新設備應上線");

        // 驗證 E10：device_events 含 replacement_item FK
        List<DeviceEvent> oldDevEvents = deviceEventRepo.findByDeviceId(oldLumId);
        boolean hasDecommission = oldDevEvents.stream()
                .anyMatch(e -> e.getEventType() == DeviceEventType.DECOMMISSION
                        && itemId.equals(e.getReplacementItemId()));
        assertTrue(hasDecommission, "舊設備應有 DECOMMISSION 事件且帶 replacement_item_id");

        List<DeviceEvent> newDevEventsAfterClose = deviceEventRepo.findByDeviceId(newDeviceId);
        boolean hasReplace = newDevEventsAfterClose.stream()
                .anyMatch(e -> e.getEventType() == DeviceEventType.REPLACE
                        && itemId.equals(e.getReplacementItemId()));
        assertTrue(hasReplace, "新設備應有 REPLACE 事件且帶 replacement_item_id");

        // 驗證 workflow 已完成
        WorkflowInstance closedWf = workflowInstanceRepo.findById(replacementWfId).orElseThrow();
        assertEquals(WorkflowStatus.COMPLETED, closedWf.getStatus());

        // 驗證 order 狀態
        order = orderRepo.findById(orderId).orElseThrow();
        assertEquals(ReplacementOrderStatus.CLOSED, order.getStatus());
    }

    // ── 輔助方法 ──

    private void lookupSeedIds() {
        loginAs("u-tpe-admin", 1L, "ALL", List.of("DEVICE_VIEW", "MATERIAL_VIEW",
                "REPLACEMENT_VIEW", "WORKFLOW_VIEW"));

        Device pole002 = deviceRepo.findByTenantIdAndDeviceCode(TENANT_A, "POLE-XINYI-002")
                .orElseThrow(() -> new RuntimeException("Seed device POLE-XINYI-002 not found"));
        poleId = pole002.getId();
        squad1DeptId = pole002.getDeptId();

        Device lum002 = deviceRepo.findByTenantIdAndDeviceCode(TENANT_A, "LUM-XINYI-002")
                .orElseThrow(() -> new RuntimeException("Seed device LUM-XINYI-002 not found"));
        oldLumId = lum002.getId();
        contractId = lum002.getContractId();

        // 查詢工程股 / 行政股 dept_id
        engDeptId = deptInfoRepo.findByTenantIdAndDeptName(TENANT_A, "工程股")
                .orElseThrow(() -> new RuntimeException("Seed dept 工程股 not found"))
                .getDeptId();
        admDeptId = deptInfoRepo.findByTenantIdAndDeptName(TENANT_A, "行政股")
                .orElseThrow(() -> new RuntimeException("Seed dept 行政股 not found"))
                .getDeptId();
        vendorDeptId = 13L; // seed 寫死

        // 查詢 MS-LED-150W inventory
        List<Inventory> allInv = inventoryRepo.findAll();
        Inventory led150Inv = allInv.stream()
                .filter(inv -> inv.getMaterialSpec() != null
                        && "MS-LED-150W".equals(inv.getMaterialSpec().getSpecCode()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Seed inventory MS-LED-150W not found"));
        inventoryId = led150Inv.getId();
        materialSpecId = led150Inv.getMaterialSpecId();
        inventoryBefore = led150Inv.getQuantityOnHand();
    }

}
