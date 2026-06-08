package com.taipei.iot.workflow.service;

import com.taipei.iot.workflow.dto.WorkflowInstanceResponse;
import com.taipei.iot.workflow.dto.WorkflowStepLogResponse;
import com.taipei.iot.workflow.entity.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface WorkflowService {

	/**
	 * 建立流程實例
	 */
	Long createInstance(String workflowType, String ticketType, Long ticketId, String creatorId);

	/**
	 * 狀態轉換（核心 FSM）
	 */
	void transition(Long instanceId, String targetStep, String action, String actorId, String actorName, String comment,
			List<java.util.Map<String, Object>> attachments);

	/**
	 * 查詢我的待辦（含代理案件）
	 */
	Page<WorkflowInstanceResponse> getMyPendingTasks(String userId, Pageable pageable);

	/**
	 * 查詢流程歷程
	 */
	List<WorkflowStepLogResponse> getStepLogs(Long instanceId);

	/**
	 * 取消流程
	 */
	void cancel(Long instanceId, String actorId);

	/**
	 * 依工單類型與工單 ID 查詢流程實例
	 */
	WorkflowInstance findByTicket(String ticketType, Long ticketId);

}
