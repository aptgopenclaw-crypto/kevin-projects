package com.taipei.iot.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.auth.security.JwtUtil;
import com.taipei.iot.common.dto.PageResponse;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.GlobalExceptionHandler;
import com.taipei.iot.config.CorsProperties;
import com.taipei.iot.config.SecurityConfig;
import com.taipei.iot.tenant.TenantEnabledCache;
import com.taipei.iot.workflow.dto.WorkflowInstanceResponse;
import com.taipei.iot.workflow.dto.WorkflowStepLogResponse;
import com.taipei.iot.workflow.dto.WorkflowTransitionRequest;
import com.taipei.iot.workflow.enums.WorkflowStatus;
import com.taipei.iot.workflow.service.WorkflowService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WorkflowController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class, CorsProperties.class })
@TestPropertySource(properties = "cors.allowed-origins=http://localhost")
class WorkflowControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private WorkflowService workflowService;

	@MockitoBean
	private JwtUtil jwtUtil;

	@MockitoBean
	private StringRedisTemplate stringRedisTemplate;

	@MockitoBean
	private TenantEnabledCache tenantEnabledCache;

	private static final String TOKEN = "test-token";

	private static final String AUTH_HEADER = "Bearer " + TOKEN;

	// ── helpers ───────────────────────────────────────────────────────────────

	private void mockJwtValid(String userId, String tenantId, List<String> permissions) {
		Map<String, Object> claimsMap = new HashMap<>();
		claimsMap.put("uid", userId);
		claimsMap.put("tenantId", tenantId);
		claimsMap.put("roles", List.of("ROLE_USER"));
		claimsMap.put("permissions", permissions);
		claimsMap.put("sub", userId);
		claimsMap.put("exp", new Date(System.currentTimeMillis() + 3_600_000));
		claimsMap.put("iat", new Date());
		Claims claims = new DefaultClaims(claimsMap);
		when(jwtUtil.parseToken(TOKEN)).thenReturn(claims);
	}

	private WorkflowInstanceResponse instanceResponse(Long id) {
		return WorkflowInstanceResponse.builder()
			.id(id)
			.workflowType("FAULT_REVIEW")
			.ticketType("FAULT")
			.ticketId(1L)
			.currentStep("OPEN")
			.status(WorkflowStatus.ACTIVE)
			.creatorId("user-1")
			.startedAt(LocalDateTime.now())
			.build();
	}

	private PageResponse<WorkflowInstanceResponse> emptyInstancePage() {
		return PageResponse.<WorkflowInstanceResponse>builder()
			.content(List.of())
			.totalElements(0)
			.totalPages(0)
			.page(0)
			.size(20)
			.build();
	}

	// ═══════════════════════════════════════════════════════════════════════════
	// GET /v1/auth/workflow/pending
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class GetPendingTasks {

		@Test
		void withPermission_returnsOk() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			when(workflowService.getMyPendingTasks(eq("user-1"), any())).thenReturn(new PageImpl<>(List.of()));

			mockMvc.perform(get("/v1/auth/workflow/pending").header("Authorization", AUTH_HEADER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errorCode").value("00000"));
		}

		@Test
		void withPermission_returnsPaginatedContent() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			WorkflowInstanceResponse inst = instanceResponse(1L);
			when(workflowService.getMyPendingTasks(eq("user-1"), any())).thenReturn(new PageImpl<>(List.of(inst)));

			mockMvc.perform(get("/v1/auth/workflow/pending").header("Authorization", AUTH_HEADER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body.content[0].id").value(1))
				.andExpect(jsonPath("$.body.content[0].workflowType").value("FAULT_REVIEW"))
				.andExpect(jsonPath("$.body.totalElements").value(1));
		}

		@Test
		void withCustomPagination_passesPageParams() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			when(workflowService.getMyPendingTasks(eq("user-1"), any())).thenReturn(new PageImpl<>(List.of()));

			mockMvc
				.perform(get("/v1/auth/workflow/pending").param("page", "2")
					.param("size", "5")
					.header("Authorization", AUTH_HEADER))
				.andExpect(status().isOk());

			verify(workflowService).getMyPendingTasks(eq("user-1"),
					argThat(p -> p.getPageNumber() == 2 && p.getPageSize() == 5));
		}

		@Test
		void unauthenticated_returns401() throws Exception {
			mockMvc.perform(get("/v1/auth/workflow/pending")).andExpect(status().isUnauthorized());
		}

		@Test
		void withoutPermission_returns403() throws Exception {
			mockJwtValid("user-1", "T1", List.of("SOME_OTHER_PERMISSION"));

			mockMvc.perform(get("/v1/auth/workflow/pending").header("Authorization", AUTH_HEADER))
				.andExpect(status().isForbidden());
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// GET /v1/auth/workflow/{instanceId}/logs
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class GetStepLogs {

		@Test
		void withPermission_returnsLogs() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			WorkflowStepLogResponse log = WorkflowStepLogResponse.builder()
				.id(10L)
				.stepCode("OPEN")
				.action("CREATE")
				.actorId("user-1")
				.isDelegated(false)
				.actedAt(LocalDateTime.now())
				.build();
			when(workflowService.getStepLogs(1L)).thenReturn(List.of(log));

			mockMvc.perform(get("/v1/auth/workflow/1/logs").header("Authorization", AUTH_HEADER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errorCode").value("00000"))
				.andExpect(jsonPath("$.body[0].stepCode").value("OPEN"))
				.andExpect(jsonPath("$.body[0].isDelegated").value(false));
		}

		@Test
		void noLogs_returnsEmptyList() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			when(workflowService.getStepLogs(99L)).thenReturn(List.of());

			mockMvc.perform(get("/v1/auth/workflow/99/logs").header("Authorization", AUTH_HEADER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.body").isArray())
				.andExpect(jsonPath("$.body").isEmpty());
		}

		@Test
		void unauthenticated_returns401() throws Exception {
			mockMvc.perform(get("/v1/auth/workflow/1/logs")).andExpect(status().isUnauthorized());
		}

		@Test
		void withoutPermission_returns403() throws Exception {
			mockJwtValid("user-1", "T1", List.of("OTHER_PERM"));

			mockMvc.perform(get("/v1/auth/workflow/1/logs").header("Authorization", AUTH_HEADER))
				.andExpect(status().isForbidden());
		}

		@Test
		void instanceNotFound_returns404() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			when(workflowService.getStepLogs(999L))
				.thenThrow(new BusinessException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND));

			mockMvc.perform(get("/v1/auth/workflow/999/logs").header("Authorization", AUTH_HEADER))
				.andExpect(status().isNotFound());
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// POST /v1/auth/workflow/{instanceId}/transition
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class Transition {

		private WorkflowTransitionRequest validRequest() {
			return WorkflowTransitionRequest.builder()
				.targetStep("REVIEW")
				.action("APPROVE")
				.comment("looks good")
				.build();
		}

		@Test
		void withPermission_validRequest_returnsOk() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doNothing().when(workflowService).transition(anyLong(), any(), any(), any(), any(), any(), any());

			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").header("Authorization", AUTH_HEADER)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errorCode").value("00000"));

			verify(workflowService).transition(eq(1L), eq("REVIEW"), eq("APPROVE"), eq("user-1"), isNull(),
					eq("looks good"), isNull());
		}

		@Test
		void missingTargetStep_returns400() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			WorkflowTransitionRequest req = WorkflowTransitionRequest.builder().action("APPROVE").build(); // targetStep
																											// missing

			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").header("Authorization", AUTH_HEADER)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest());
		}

		@Test
		void missingAction_returns400() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			WorkflowTransitionRequest req = WorkflowTransitionRequest.builder().targetStep("REVIEW").build(); // action
																												// missing

			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").header("Authorization", AUTH_HEADER)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(req)))
				.andExpect(status().isBadRequest());
		}

		@Test
		void invalidTransition_returns400() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doThrow(new BusinessException(ErrorCode.WORKFLOW_INVALID_TRANSITION)).when(workflowService)
				.transition(anyLong(), any(), any(), any(), any(), any(), any());

			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").header("Authorization", AUTH_HEADER)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isBadRequest());
		}

		@Test
		void selfApproval_returns400() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doThrow(new BusinessException(ErrorCode.WORKFLOW_SELF_APPROVAL_NOT_ALLOWED)).when(workflowService)
				.transition(anyLong(), any(), any(), any(), any(), any(), any());

			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").header("Authorization", AUTH_HEADER)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isBadRequest());
		}

		@Test
		void notAssignedUser_returns403() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doThrow(new BusinessException(ErrorCode.WORKFLOW_NOT_ASSIGNED_TO_USER)).when(workflowService)
				.transition(anyLong(), any(), any(), any(), any(), any(), any());

			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").header("Authorization", AUTH_HEADER)
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isForbidden());
		}

		@Test
		void unauthenticated_returns401() throws Exception {
			mockMvc
				.perform(post("/v1/auth/workflow/1/transition").contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(validRequest())))
				.andExpect(status().isUnauthorized());
		}

	}

	// ═══════════════════════════════════════════════════════════════════════════
	// POST /v1/auth/workflow/{instanceId}/cancel
	// ═══════════════════════════════════════════════════════════════════════════

	@Nested
	class Cancel {

		@Test
		void withPermission_returnsOk() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doNothing().when(workflowService).cancel(anyLong(), any());

			mockMvc.perform(post("/v1/auth/workflow/1/cancel").header("Authorization", AUTH_HEADER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.errorCode").value("00000"));

			verify(workflowService).cancel(eq(1L), eq("user-1"));
		}

		@Test
		void instanceNotFound_returns404() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doThrow(new BusinessException(ErrorCode.WORKFLOW_INSTANCE_NOT_FOUND)).when(workflowService)
				.cancel(anyLong(), any());

			mockMvc.perform(post("/v1/auth/workflow/99/cancel").header("Authorization", AUTH_HEADER))
				.andExpect(status().isNotFound());
		}

		@Test
		void alreadyCompleted_returns400() throws Exception {
			mockJwtValid("user-1", "T1", List.of("WORKFLOW_VIEW"));
			doThrow(new BusinessException(ErrorCode.WORKFLOW_INVALID_TRANSITION, "流程已結束")).when(workflowService)
				.cancel(anyLong(), any());

			mockMvc.perform(post("/v1/auth/workflow/1/cancel").header("Authorization", AUTH_HEADER))
				.andExpect(status().isBadRequest());
		}

		@Test
		void unauthenticated_returns401() throws Exception {
			mockMvc.perform(post("/v1/auth/workflow/1/cancel")).andExpect(status().isUnauthorized());
		}

		@Test
		void withoutPermission_returns403() throws Exception {
			mockJwtValid("user-1", "T1", List.of("OTHER_PERM"));

			mockMvc.perform(post("/v1/auth/workflow/1/cancel").header("Authorization", AUTH_HEADER))
				.andExpect(status().isForbidden());
		}

	}

}
