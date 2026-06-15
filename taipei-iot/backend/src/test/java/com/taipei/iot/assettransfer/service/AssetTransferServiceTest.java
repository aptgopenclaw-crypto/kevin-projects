package com.taipei.iot.assettransfer.service;

import com.taipei.iot.assettransfer.dto.AssetTransferCreateRequest;
import com.taipei.iot.assettransfer.dto.AssetTransferResponse;
import com.taipei.iot.assettransfer.entity.AssetTransferApplicationEntity;
import com.taipei.iot.assettransfer.enums.AssetTransferStatus;
import com.taipei.iot.assettransfer.repository.AssetTransferApplicationRepository;
import com.taipei.iot.auth.entity.UserEntity;
import com.taipei.iot.auth.repository.UserRepository;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.dept.entity.DeptInfoEntity;
import com.taipei.iot.dept.repository.DeptInfoRepository;
import com.taipei.iot.tenant.TenantContext;
import com.taipei.iot.workflow.entity.WorkflowInstanceEntity;
import com.taipei.iot.workflow.entity.WorkflowStepLogEntity;
import com.taipei.iot.workflow.model.WorkflowContext;
import com.taipei.iot.workflow.repository.WorkflowStepLogRepository;
import com.taipei.iot.workflow.service.WorkflowEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AssetTransferServiceTest {

	@InjectMocks
	private AssetTransferService service;

	@Mock
	private AssetTransferApplicationRepository repository;

	@Mock
	private WorkflowEngine workflowEngine;

	@Mock
	private WorkflowStepLogRepository stepLogRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private DeptInfoRepository deptInfoRepository;

	// ─── Fixture helpers ──────────────────────────────────────────────────────

	private static final String TENANT = "TENANT_A";

	private static final String APPLICANT_ID = "user-001";

	private static final String APPROVER_ID = "approver-001";

	private static final Long APP_ID = 1L;

	private static final Long WORKFLOW_ID = 10L;

	private AssetTransferApplicationEntity draftApp() {
		return AssetTransferApplicationEntity.builder()
			.id(APP_ID)
			.tenantId(TENANT)
			.applicationNo("AT-DRAFT")
			.applicantId(APPLICANT_ID)
			.departmentId(100L)
			.assetCode("AC-001")
			.assetName("Laptop")
			.transferType("INTERNAL")
			.status(AssetTransferStatus.DRAFT)
			.build();
	}

	private AssetTransferApplicationEntity processingApp(String currentAssignee) {
		return AssetTransferApplicationEntity.builder()
			.id(APP_ID)
			.tenantId(TENANT)
			.applicationNo("AT-PROC")
			.applicantId(APPLICANT_ID)
			.departmentId(100L)
			.workflowInstanceId(WORKFLOW_ID)
			.assetCode("AC-001")
			.assetName("Laptop")
			.transferType("INTERNAL")
			.status(AssetTransferStatus.PROCESSING)
			.currentAssignee(currentAssignee)
			.build();
	}

	private AssetTransferApplicationEntity rejectedApp() {
		return AssetTransferApplicationEntity.builder()
			.id(APP_ID)
			.tenantId(TENANT)
			.applicationNo("AT-REJ")
			.applicantId(APPLICANT_ID)
			.departmentId(100L)
			.workflowInstanceId(WORKFLOW_ID)
			.assetCode("AC-001")
			.assetName("Laptop")
			.transferType("INTERNAL")
			.status(AssetTransferStatus.REJECTED)
			.rejectReason("缺件")
			.build();
	}

	private WorkflowInstanceEntity workflowInstance(String status) {
		WorkflowInstanceEntity inst = new WorkflowInstanceEntity();
		inst.setId(WORKFLOW_ID);
		inst.setStatus(status);
		return inst;
	}

	@BeforeEach
	void setUp() {
		TenantContext.setCurrentTenantId(TENANT);
		// toResponse() 中解析 assigneeName
		when(userRepository.findById(anyString()))
			.thenReturn(Optional.of(UserEntity.builder().displayName("顯示名稱").build()));
		// 大部分測試不需要 stepLog；個別測試可覆寫
		when(stepLogRepository.findCurrentByInstanceId(anyLong())).thenReturn(Optional.empty());
	}

	@AfterEach
	void tearDown() {
		TenantContext.clear();
	}

	// ═══════════════════════════════════════════════════════════════════════
	// create()
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	class CreateTests {

		private AssetTransferCreateRequest validReq() {
			return new AssetTransferCreateRequest("AC-001", "Laptop", "INTERNAL", 100L, null, "reason",
					BigDecimal.valueOf(50000));
		}

		@BeforeEach
		void mockDept() {
			when(deptInfoRepository.findByDeptId(100L))
				.thenReturn(Optional.of(DeptInfoEntity.builder().deptName("IT部門").build()));
		}

		@Test
		void create_validRequest_savesAndReturnsDraft() {
			when(repository.save(any())).thenAnswer(inv -> {
				AssetTransferApplicationEntity a = inv.getArgument(0);
				a.setId(APP_ID);
				return a;
			});

			AssetTransferResponse resp = service.create(validReq(), APPLICANT_ID);

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.DRAFT);
			assertThat(resp.departmentName()).isEqualTo("IT部門");
			verify(repository).save(any());
		}

		@Test
		void create_applicationNoUsesUUID_notTimestamp() {
			when(repository.save(any())).thenAnswer(inv -> {
				AssetTransferApplicationEntity a = inv.getArgument(0);
				a.setId(APP_ID);
				return a;
			});

			service.create(validReq(), APPLICANT_ID);

			verify(repository).save(argThat(a -> a.getApplicationNo() != null && a.getApplicationNo().startsWith("AT-")
					&& a.getApplicationNo().length() == 19 // "AT-" + 16 chars
			));
		}

		@Test
		void create_unknownDeptId_throwsDeptNotFound() {
			when(deptInfoRepository.findByDeptId(100L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.create(validReq(), APPLICANT_ID)).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.DEPT_NOT_FOUND);
		}

		@Test
		void create_targetDeptIdBelongsOtherTenant_throwsDeptNotFound() {
			AssetTransferCreateRequest reqWithTarget = new AssetTransferCreateRequest("AC-001", "Laptop", "INTERNAL",
					100L, 999L, null, null);
			when(deptInfoRepository.findByDeptId(999L)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.create(reqWithTarget, APPLICANT_ID)).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.DEPT_NOT_FOUND);
		}

	}

	// ═══════════════════════════════════════════════════════════════════════
	// submit()
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	class SubmitTests {

		@Test
		void submit_byApplicant_draftApp_succeeds() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(draftApp()));
			WorkflowInstanceEntity inst = workflowInstance("PROCESSING");
			when(workflowEngine.start(anyString(), anyString(), anyString(), any(WorkflowContext.class)))
				.thenReturn(inst);
			when(workflowEngine.approve(WORKFLOW_ID, "申請人送出", APPLICANT_ID)).thenReturn(inst);
			when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			AssetTransferResponse resp = service.submit(APP_ID, APPLICANT_ID);

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.PROCESSING);
		}

		@Test
		void submit_byNonApplicant_throwsPermissionDenied() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(draftApp()));

			assertThatThrownBy(() -> service.submit(APP_ID, "other-user")).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_PERMISSION_DENIED);
		}

		@Test
		void submit_nonDraftApp_throwsInvalidStatus() {
			AssetTransferApplicationEntity processing = processingApp(APPLICANT_ID);
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processing));

			assertThatThrownBy(() -> service.submit(APP_ID, APPLICANT_ID)).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_INVALID_STATUS);
		}

	}

	// ═══════════════════════════════════════════════════════════════════════
	// approve()
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	class ApproveTests {

		@Test
		void approve_byCurrentAssignee_processing_succeeds() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processingApp(APPROVER_ID)));
			WorkflowInstanceEntity inst = workflowInstance("PROCESSING");
			when(workflowEngine.approve(WORKFLOW_ID, "ok", APPROVER_ID)).thenReturn(inst);
			when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			AssetTransferResponse resp = service.approve(APP_ID, APPROVER_ID, "ok");

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.PROCESSING);
		}

		@Test
		void approve_lastStep_setsCompleted() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processingApp(APPROVER_ID)));
			WorkflowInstanceEntity inst = workflowInstance("COMPLETED");
			when(workflowEngine.approve(WORKFLOW_ID, "final", APPROVER_ID)).thenReturn(inst);
			when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			AssetTransferResponse resp = service.approve(APP_ID, APPROVER_ID, "final");

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.COMPLETED);
		}

		@Test
		void approve_byNonCurrentAssignee_throwsPermissionDenied() {
			// currentAssignee = APPROVER_ID, but called by "attacker"
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processingApp(APPROVER_ID)));

			assertThatThrownBy(() -> service.approve(APP_ID, "attacker", "hack")).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_PERMISSION_DENIED);

			verify(workflowEngine, never()).approve(anyLong(), anyString(), anyString());
		}

		@Test
		void approve_notProcessingApp_throwsInvalidStatus() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(draftApp()));

			assertThatThrownBy(() -> service.approve(APP_ID, APPROVER_ID, "ok")).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_INVALID_STATUS);
		}

	}

	// ═══════════════════════════════════════════════════════════════════════
	// reject()
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	class RejectTests {

		@Test
		void reject_byCurrentAssignee_toApplicant_setsRejected() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processingApp(APPROVER_ID)));
			WorkflowInstanceEntity inst = workflowInstance("PROCESSING");
			when(workflowEngine.reject(WORKFLOW_ID, "step-1", "缺件", APPROVER_ID)).thenReturn(inst);
			// reject target is the applicant → status becomes REJECTED
			WorkflowStepLogEntity log = new WorkflowStepLogEntity();
			log.setAssigneeUserId(APPLICANT_ID);
			when(stepLogRepository.findCurrentByInstanceId(WORKFLOW_ID)).thenReturn(Optional.of(log));
			when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			AssetTransferResponse resp = service.reject(APP_ID, APPROVER_ID, "缺件", "step-1");

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.REJECTED);
			assertThat(resp.rejectReason()).isEqualTo("缺件");
		}

		@Test
		void reject_toIntermediateStep_remainsProcessing() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processingApp(APPROVER_ID)));
			WorkflowInstanceEntity inst = workflowInstance("PROCESSING");
			when(workflowEngine.reject(WORKFLOW_ID, "step-dept-admin", "補件", APPROVER_ID)).thenReturn(inst);
			WorkflowStepLogEntity log = new WorkflowStepLogEntity();
			log.setAssigneeUserId("dept-admin-001"); // not the applicant
			when(stepLogRepository.findCurrentByInstanceId(WORKFLOW_ID)).thenReturn(Optional.of(log));
			when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			AssetTransferResponse resp = service.reject(APP_ID, APPROVER_ID, "補件", "step-dept-admin");

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.PROCESSING);
		}

		@Test
		void reject_byNonCurrentAssignee_throwsPermissionDenied() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(processingApp(APPROVER_ID)));

			assertThatThrownBy(() -> service.reject(APP_ID, "attacker", "hack", "step-1"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_PERMISSION_DENIED);

			verify(workflowEngine, never()).reject(anyLong(), anyString(), anyString(), anyString());
		}

	}

	// ═══════════════════════════════════════════════════════════════════════
	// resubmit()
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	class ResubmitTests {

		@Test
		void resubmit_byApplicant_rejectedApp_succeeds() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(rejectedApp()));
			WorkflowInstanceEntity inst = workflowInstance("PROCESSING");
			when(workflowEngine.resubmit(WORKFLOW_ID, "補件完成", APPLICANT_ID)).thenReturn(inst);
			when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

			AssetTransferResponse resp = service.resubmit(APP_ID, APPLICANT_ID, "補件完成");

			assertThat(resp.status()).isEqualTo(AssetTransferStatus.PROCESSING);
			assertThat(resp.rejectReason()).isNull();
		}

		@Test
		void resubmit_byNonApplicant_throwsPermissionDenied() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(rejectedApp()));

			assertThatThrownBy(() -> service.resubmit(APP_ID, "other-user", "hack"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_PERMISSION_DENIED);

			verify(workflowEngine, never()).resubmit(anyLong(), anyString(), anyString());
		}

		@Test
		void resubmit_appWithNoWorkflow_throwsWorkflowNotStarted() {
			AssetTransferApplicationEntity noWorkflow = AssetTransferApplicationEntity.builder()
				.id(APP_ID)
				.applicantId(APPLICANT_ID)
				.status(AssetTransferStatus.REJECTED)
				.workflowInstanceId(null)
				.build();
			when(repository.findById(APP_ID)).thenReturn(Optional.of(noWorkflow));

			assertThatThrownBy(() -> service.resubmit(APP_ID, APPLICANT_ID, "ok")).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_WORKFLOW_NOT_STARTED);
		}

	}

	// ═══════════════════════════════════════════════════════════════════════
	// getById() / getMyApplications()
	// ═══════════════════════════════════════════════════════════════════════

	@Nested
	class QueryTests {

		@Test
		void getById_exists_returnsResponse() {
			when(repository.findById(APP_ID)).thenReturn(Optional.of(draftApp()));

			AssetTransferResponse resp = service.getById(APP_ID);

			assertThat(resp.id()).isEqualTo(APP_ID);
			assertThat(resp.status()).isEqualTo(AssetTransferStatus.DRAFT);
		}

		@Test
		void getById_notFound_throwsNotFound() {
			when(repository.findById(APP_ID)).thenReturn(Optional.empty());

			assertThatThrownBy(() -> service.getById(APP_ID)).isInstanceOf(BusinessException.class)
				.extracting("errorCode")
				.isEqualTo(ErrorCode.ASSET_TRANSFER_NOT_FOUND);
		}

		@Test
		void getMyApplications_returnsAll() {
			when(repository.findByApplicantIdOrderByCreatedAtDesc(APPLICANT_ID))
				.thenReturn(List.of(draftApp(), processingApp(APPROVER_ID)));

			List<AssetTransferResponse> result = service.getMyApplications(APPLICANT_ID);

			assertThat(result).hasSize(2);
		}

	}

}
