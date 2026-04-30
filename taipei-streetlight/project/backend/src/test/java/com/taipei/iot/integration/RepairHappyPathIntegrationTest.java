package com.taipei.iot.integration;

import com.taipei.iot.device.entity.Device;
import com.taipei.iot.device.enums.DeviceEventType;
import com.taipei.iot.device.enums.DeviceStatus;
import com.taipei.iot.device.repository.DeviceEventRepository;
import com.taipei.iot.device.repository.DeviceRepository;
import com.taipei.iot.fault.dto.FaultTicketRequest;
import com.taipei.iot.fault.dto.FaultTicketResponse;
import com.taipei.iot.fault.enums.FaultTicketSource;
import com.taipei.iot.fault.enums.FaultTicketStatus;
import com.taipei.iot.fault.repository.FaultTicketRepository;
import com.taipei.iot.fault.service.FaultTicketService;
import com.taipei.iot.repair.dto.CompletionReportRequest;
import com.taipei.iot.repair.dto.DispatchRequest;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.enums.RepairTicketStatus;
import com.taipei.iot.repair.repository.RepairDispatchRepository;
import com.taipei.iot.repair.repository.RepairTicketRepository;
import com.taipei.iot.repair.service.RepairDispatchService;
import com.taipei.iot.repair.service.RepairTicketService;
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
 * Happy Path 1：故障報修閉環 — 信義路三段 #001 燈桿不亮
 * <p>
 * 流程：障礙通報 → 審核(E1) → 收案 → 派工(E4) → 施工 → 完工回報(E8) → 結案(E9)
 * <p>
 * 事件鏈：E1→E4→E8→E9，設備最終恢復 ACTIVE
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepairHappyPathIntegrationTest {

    @Autowired private FaultTicketService faultTicketService;
    @Autowired private RepairTicketService repairTicketService;
    @Autowired private RepairDispatchService repairDispatchService;
    @Autowired private WorkflowService workflowService;

    @Autowired private FaultTicketRepository faultTicketRepository;
    @Autowired private RepairTicketRepository repairTicketRepository;
    @Autowired private RepairDispatchRepository repairDispatchRepository;
    @Autowired private DeviceRepository deviceRepository;
    @Autowired private DeviceEventRepository deviceEventRepository;
    @Autowired private WorkflowInstanceRepository workflowInstanceRepository;
    @Autowired private WorkflowStepLogRepository workflowStepLogRepository;

    // 動態查詢的 ID
    private Long squad1DeptId;
    private Long deviceId; // LUM-XINYI-001
    private Long poleId;   // POLE-XINYI-001
    private Long contractId;

    // 測試過程中產生的 ID（需 cleanup）
    private Long faultTicketId;
    private Long faultReviewWfId;
    private Long repairTicketId;
    private Long repairDispatchWfId;
    private Long repairCloseWfId;

    @BeforeEach
    void setUp() {
        TenantContext_set();
        lookupSeedIds();
    }

    @AfterEach
    void tearDown() {
        try {
            TenantContext_set();
            // 清理測試資料（逆向刪除 FK 依賴順序）
            if (repairCloseWfId != null) cleanupWorkflow(repairCloseWfId);
            if (repairDispatchWfId != null) cleanupWorkflow(repairDispatchWfId);
            if (faultReviewWfId != null) cleanupWorkflow(faultReviewWfId);
            if (repairTicketId != null) {
                deviceEventRepository.deleteByRepairTicketId(repairTicketId);
                repairDispatchRepository.deleteByRepairTicketId(repairTicketId);
                repairTicketRepository.deleteById(repairTicketId);
            }
            if (faultTicketId != null) faultTicketRepository.deleteById(faultTicketId);
            // 恢復設備狀態
            if (deviceId != null) resetDeviceStatus(deviceId);
        } finally {
            IntegrationTestHelper.logout();
        }
    }

    /**
     * 完整 Happy Path 1：故障報修閉環
     */
    @Test
    @Order(1)
    void happyPath1_repairClosedLoop() {
        // ═══ Step 1: 張志遠建立障礙通報 ═══
        loginAsSquad1Off1(squad1DeptId);

        FaultTicketRequest faultRequest = FaultTicketRequest.builder()
                .deviceId(deviceId)
                .source(FaultTicketSource.CITIZEN_REPORT)
                .priority("NORMAL")
                .description("信義路三段 #001 燈桿不亮，疑似電線鬆脫")
                .build();
        FaultTicketResponse faultResp = faultTicketService.create(faultRequest);
        faultTicketId = faultResp.getId();

        assertNotNull(faultTicketId);
        assertEquals(FaultTicketStatus.OPEN, faultResp.getStatus());
        assertEquals(deviceId, faultResp.getDeviceId());

        // 手動建立 FAULT_REVIEW 工作流（prod 尚未串接）
        faultReviewWfId = workflowService.createInstance(
                "FAULT_REVIEW", "FAULT_TICKET", faultTicketId, "u-squad1-off1");

        // ═══ Step 2: 周志豪審核確認障礙 → E1 自動建報修 ═══
        loginAsSquad1Op(squad1DeptId);

        // OPEN → REVIEW
        workflowService.transition(faultReviewWfId, "REVIEW", "送審",
                "u-squad1-op", "周志豪", "送審檢視", null);

        // REVIEW → CONFIRMED (觸發 E1: FaultApprovedListener)
        workflowService.transition(faultReviewWfId, "CONFIRMED", "確認",
                "u-squad1-op", "周志豪", "確認為真實障礙", null);

        // 驗證 E1：自動建立報修工單
        List<RepairTicket> repairTickets = repairTicketRepository.findAll().stream()
                .filter(rt -> faultTicketId.equals(rt.getFaultTicketId()))
                .toList();
        assertEquals(1, repairTickets.size(), "E1 應自動建立 1 張報修工單");
        RepairTicket repairTicket = repairTickets.get(0);
        repairTicketId = repairTicket.getId();
        assertEquals(RepairTicketStatus.PENDING, repairTicket.getStatus());
        assertEquals(deviceId, repairTicket.getDeviceId());

        // 驗證 E1：設備狀態 → REPORTED
        Device device = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals(DeviceStatus.REPORTED, device.getStatus(), "E1 應將設備狀態更新為 REPORTED");

        // 取得自動建立的 REPAIR_DISPATCH workflow
        WorkflowInstance repairWf = workflowService.findByTicket("REPAIR_TICKET", repairTicketId);
        repairDispatchWfId = repairWf.getId();
        assertEquals("PENDING", repairWf.getCurrentStep());

        // ═══ Step 3: 周志豪收案 → 派工給蔡文傑 → E4 ═══
        repairTicketService.accept(repairTicketId);

        // 驗證收案
        repairWf = workflowInstanceRepository.findById(repairDispatchWfId).orElseThrow();
        assertEquals("ACCEPTED", repairWf.getCurrentStep());

        // 派工
        DispatchRequest dispatchReq = DispatchRequest.builder()
                .assignedTo(null)  // 由 assignedOrg 描述
                .assignedOrg("蔡文傑（北區外勤）")
                .contractId(contractId)
                .dueDate(LocalDate.now().plusDays(3))
                .note("請盡速維修，信義路三段民眾反映")
                .build();
        repairDispatchService.dispatch(repairTicketId, dispatchReq);

        // 驗證 E4：設備狀態 → UNDER_REPAIR
        device = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals(DeviceStatus.UNDER_REPAIR, device.getStatus(), "E4 應將設備狀態更新為 UNDER_REPAIR");

        repairWf = workflowInstanceRepository.findById(repairDispatchWfId).orElseThrow();
        assertEquals("DISPATCHED", repairWf.getCurrentStep());

        // ═══ Step 4: 蔡文傑 DISPATCHED → IN_PROGRESS → 完工回報 ═══
        loginAsSquad1Field(squad1DeptId);

        // DISPATCHED → IN_PROGRESS
        workflowService.transition(repairDispatchWfId, "IN_PROGRESS", "開工",
                "u-squad1-field", "蔡文傑", "已到達現場，開始施工", null);

        // 完工回報
        CompletionReportRequest completionReq = CompletionReportRequest.builder()
                .repairDescription("電線鬆脫重新接回，燈具恢復正常亮燈")
                .faultCause("電線接點氧化鬆脫")
                .build();
        repairTicketService.reportCompletion(repairTicketId, completionReq);

        // 驗證 REPAIR_DISPATCH 到達 COMPLETION_REPORTED
        repairWf = workflowInstanceRepository.findById(repairDispatchWfId).orElseThrow();
        assertEquals("COMPLETION_REPORTED", repairWf.getCurrentStep());

        // ═══ Step 5: 李明華結案審核 → E9 設備恢復 ACTIVE ═══
        loginAsSquad1Mgr(squad1DeptId);

        // 建立 REPAIR_CLOSE 工作流（prod 尚未串接，需手動建立）
        repairCloseWfId = workflowService.createInstance(
                "REPAIR_CLOSE", "REPAIR_TICKET", repairTicketId, "u-squad1-field");

        // COMPLETION_REPORTED → PENDING_REVIEW
        workflowService.transition(repairCloseWfId, "PENDING_REVIEW", "送審",
                "u-squad1-mgr", "李明華", "完工報告確認中", null);

        // PENDING_REVIEW → CLOSED (觸發 E9: RepairClosedListener)
        workflowService.transition(repairCloseWfId, "CLOSED", "結案",
                "u-squad1-mgr", "李明華", "確認完工品質良好，核准結案", null);

        // 驗證 E9：設備狀態 → ACTIVE
        device = deviceRepository.findById(deviceId).orElseThrow();
        assertEquals(DeviceStatus.ACTIVE, device.getStatus(), "E9 應將設備狀態恢復為 ACTIVE");

        // 驗證 E9：device_events 含 repair_ticket_id FK
        var events = deviceEventRepository.findByDeviceId(deviceId);
        boolean hasRepairEvent = events.stream()
                .anyMatch(e -> e.getEventType() == DeviceEventType.REPAIR
                        && repairTicketId.equals(e.getRepairTicketId()));
        assertTrue(hasRepairEvent, "device_events 應含 REPAIR 事件且帶有 repair_ticket_id FK");

        // 驗證 workflow 已完成
        WorkflowInstance closedWf = workflowInstanceRepository.findById(repairCloseWfId).orElseThrow();
        assertEquals(WorkflowStatus.COMPLETED, closedWf.getStatus());
        assertEquals("CLOSED", closedWf.getCurrentStep());
    }

    // ── 輔助方法 ──

    private void TenantContext_set() {
        com.taipei.iot.tenant.TenantContext.setCurrentTenantId(TENANT_A);
    }

    private void lookupSeedIds() {
        loginAs("u-tpe-admin", 1L, "ALL",
                List.of("FAULT_VIEW", "FAULT_MANAGE", "REPAIR_VIEW", "REPAIR_MANAGE",
                        "DEVICE_VIEW", "DEVICE_MANAGE", "WORKFLOW_VIEW", "WORKFLOW_MANAGE"));

        // 查詢設備 ID
        Device lum001 = deviceRepository.findByTenantIdAndDeviceCode(TENANT_A, "LUM-XINYI-001")
                .orElseThrow(() -> new RuntimeException("Seed device LUM-XINYI-001 not found"));
        deviceId = lum001.getId();
        poleId = lum001.getParentDeviceId();
        squad1DeptId = lum001.getDeptId();
        contractId = lum001.getContractId();
    }

    private void cleanupWorkflow(Long workflowId) {
        workflowStepLogRepository.deleteByInstanceId(workflowId);
        workflowInstanceRepository.deleteById(workflowId);
    }

    private void resetDeviceStatus(Long devId) {
        deviceRepository.findById(devId).ifPresent(d -> {
            d.setStatus(DeviceStatus.ACTIVE);
            deviceRepository.save(d);
        });
    }
}
