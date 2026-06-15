package com.taipei.iot.assettransfer.controller;

import com.taipei.iot.assettransfer.dto.AssetTransferActionRequest;
import com.taipei.iot.assettransfer.dto.AssetTransferCreateRequest;
import com.taipei.iot.assettransfer.dto.AssetTransferRejectRequest;
import com.taipei.iot.assettransfer.dto.RejectTargetDto;
import com.taipei.iot.assettransfer.entity.AssetTransferApplicationEntity;
import com.taipei.iot.assettransfer.service.AssetTransferService;
import com.taipei.iot.audit.annotation.AuditEvent;
import com.taipei.iot.audit.enums.AuditEventType;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.util.SecurityContextUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 資產異動申請 REST API。
 * <p>
 * 端點一覽：<br>
 * POST /api/asset-transfer/create — 建立申請（草稿）<br>
 * POST /api/asset-transfer/submit/{id} — 送出申請（啟動流程）<br>
 * POST /api/asset-transfer/approve/{id} — 審核通過<br>
 * POST /api/asset-transfer/reject/{id} — 審核退回<br>
 * POST /api/asset-transfer/resubmit/{id} — 補件重送<br>
 * GET /api/asset-transfer/{id} — 查詢申請明細<br>
 * GET /api/asset-transfer/my — 查詢我的申請<br>
 * GET /api/asset-transfer/pending — 查詢我的待審案件
 */
@RestController
@RequestMapping("/v1/auth/asset-transfer")
@RequiredArgsConstructor
@Tag(name = "AssetTransfer", description = "資產異動申請：草稿建立、送出、審核、退回與重送")
public class AssetTransferController {

	private final AssetTransferService service;

	@PostMapping("/create")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_CREATE')")
	@AuditEvent(AuditEventType.CREATE_ASSET_TRANSFER)
	@Operation(summary = "建立資產異動申請", description = "建立新的資產異動申請草稿，回傳建立後的申請資料")
	public BaseResponse<AssetTransferApplicationEntity> create(@Valid @RequestBody AssetTransferCreateRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.create(req, currentUserId));
	}

	@PostMapping("/submit/{id}")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_CREATE')")
	@AuditEvent(AuditEventType.WORKFLOW_SUBMIT)
	@Operation(summary = "送出資產異動申請", description = "將草稿送出並啟動簽核流程")
	public BaseResponse<AssetTransferApplicationEntity> submit(@PathVariable Long id) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.submit(id, currentUserId));
	}

	@PostMapping("/approve/{id}")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_APPROVE')")
	@AuditEvent(AuditEventType.WORKFLOW_APPROVE)
	@Operation(summary = "審核通過資產異動申請", description = "核准目前簽核步驟，並推進到下一步")
	public BaseResponse<AssetTransferApplicationEntity> approve(@PathVariable Long id,
			@Valid @RequestBody AssetTransferActionRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.approve(id, currentUserId, req.comment()));
	}

	@PostMapping("/reject/{id}")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_APPROVE')")
	@AuditEvent(AuditEventType.WORKFLOW_REJECT)
	@Operation(summary = "退回資產異動申請", description = "將申請退回到指定步驟；targetStepId 需符合流程定義中的 reject_target")
	public BaseResponse<AssetTransferApplicationEntity> reject(@PathVariable Long id,
			@Valid @RequestBody AssetTransferRejectRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.reject(id, currentUserId, req.comment(), req.targetStepId()));
	}

	@PostMapping("/resubmit/{id}")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_CREATE')")
	@AuditEvent(AuditEventType.WORKFLOW_RESUBMIT)
	@Operation(summary = "補件重送資產異動申請", description = "將已退回的申請重送回流程，回到最近一次退回來源步驟")
	public BaseResponse<AssetTransferApplicationEntity> resubmit(@PathVariable Long id,
			@Valid @RequestBody AssetTransferActionRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.resubmit(id, currentUserId, req.comment()));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_VIEW')")
	@Operation(summary = "查詢資產異動申請明細", description = "依申請 ID 查詢單筆申請資料與目前流程狀態")
	public BaseResponse<AssetTransferApplicationEntity> getById(@PathVariable Long id) {
		return BaseResponse.success(service.getById(id));
	}

	@GetMapping("/{id}/reject-targets")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_APPROVE')")
	@Operation(summary = "查詢可退回步驟", description = "回傳此筆申請目前步驟允許退回的目標步驟清單")
	public BaseResponse<List<RejectTargetDto>> getRejectTargets(@PathVariable Long id) {
		return BaseResponse.success(service.getRejectTargets(id));
	}

	@GetMapping("/my")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_VIEW')")
	@Operation(summary = "查詢我的資產異動申請", description = "列出目前登入者建立的資產異動申請清單")
	public BaseResponse<List<AssetTransferApplicationEntity>> myApplications() {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.getMyApplications(currentUserId));
	}

	@GetMapping("/pending")
	@PreAuthorize("hasAuthority('ASSET_TRANSFER_APPROVE')")
	@Operation(summary = "查詢我的待審案件", description = "列出目前登入者可處理的資產異動待審案件")
	public BaseResponse<List<AssetTransferApplicationEntity>> pending() {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.getPendingTasks(currentUserId));
	}

}
