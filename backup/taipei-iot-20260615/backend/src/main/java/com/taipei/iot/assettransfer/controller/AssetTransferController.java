package com.taipei.iot.assettransfer.controller;

import com.taipei.iot.assettransfer.dto.AssetTransferActionRequest;
import com.taipei.iot.assettransfer.dto.AssetTransferCreateRequest;
import com.taipei.iot.assettransfer.dto.AssetTransferRejectRequest;
import com.taipei.iot.assettransfer.entity.AssetTransferApplicationEntity;
import com.taipei.iot.assettransfer.service.AssetTransferService;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.common.util.SecurityContextUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class AssetTransferController {

	private final AssetTransferService service;

	@PostMapping("/create")
	public BaseResponse<AssetTransferApplicationEntity> create(@Valid @RequestBody AssetTransferCreateRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.create(req, currentUserId));
	}

	@PostMapping("/submit/{id}")
	public BaseResponse<AssetTransferApplicationEntity> submit(@PathVariable Long id) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.submit(id, currentUserId));
	}

	@PostMapping("/approve/{id}")
	public BaseResponse<AssetTransferApplicationEntity> approve(@PathVariable Long id,
			@Valid @RequestBody AssetTransferActionRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.approve(id, currentUserId, req.comment()));
	}

	@PostMapping("/reject/{id}")
	public BaseResponse<AssetTransferApplicationEntity> reject(@PathVariable Long id,
			@Valid @RequestBody AssetTransferRejectRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.reject(id, currentUserId, req.comment(), req.targetStepId()));
	}

	@PostMapping("/resubmit/{id}")
	public BaseResponse<AssetTransferApplicationEntity> resubmit(@PathVariable Long id,
			@Valid @RequestBody AssetTransferActionRequest req) {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.resubmit(id, currentUserId, req.comment()));
	}

	@GetMapping("/{id}")
	public BaseResponse<AssetTransferApplicationEntity> getById(@PathVariable Long id) {
		return BaseResponse.success(service.getById(id));
	}

	@GetMapping("/my")
	public BaseResponse<List<AssetTransferApplicationEntity>> myApplications() {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.getMyApplications(currentUserId));
	}

	@GetMapping("/pending")
	public BaseResponse<List<AssetTransferApplicationEntity>> pending() {
		String currentUserId = SecurityContextUtils.requireCurrentUserIdStrict();
		return BaseResponse.success(service.getPendingTasks(currentUserId));
	}

}
