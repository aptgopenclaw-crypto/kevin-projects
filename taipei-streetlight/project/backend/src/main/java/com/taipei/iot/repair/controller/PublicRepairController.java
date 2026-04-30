package com.taipei.iot.repair.controller;

import com.taipei.iot.auth.service.CaptchaService;
import com.taipei.iot.common.annotation.RateLimit;
import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.repair.dto.PublicRepairRequest;
import com.taipei.iot.repair.dto.PublicRepairResponse;
import com.taipei.iot.repair.dto.PublicRepairStatusResponse;
import com.taipei.iot.repair.entity.RepairTicket;
import com.taipei.iot.repair.service.RepairTicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/noauth/public/repair")
@RequiredArgsConstructor
public class PublicRepairController {

    private final RepairTicketService repairTicketService;
    private final CaptchaService captchaService;

    @PostMapping
    @RateLimit(key = "public-repair", limit = 3, period = 300)
    public BaseResponse<PublicRepairResponse> submitRepair(@Valid @RequestBody PublicRepairRequest request) {
        if (!request.isPrivacyAgreed()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "請同意個資使用聲明");
        }

        if (!captchaService.verify(request.getCaptchaKey(), request.getCaptchaValue())) {
            throw new BusinessException(ErrorCode.CAPTCHA_INVALID);
        }

        RepairTicket ticket = repairTicketService.createPublicTicket(request);

        return BaseResponse.success(PublicRepairResponse.builder()
                .ticketNumber(ticket.getTicketNumber())
                .message("報修成功，請記錄案件編號以查詢進度")
                .build());
    }

    @GetMapping("/{ticketNo}/status")
    @RateLimit(key = "public-repair-status", limit = 10, period = 60)
    public BaseResponse<PublicRepairStatusResponse> queryStatus(
            @PathVariable String ticketNo,
            @RequestParam String phone) {
        return BaseResponse.success(repairTicketService.getPublicStatus(ticketNo, phone));
    }
}
