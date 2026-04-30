package com.taipei.iot.replacement.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.replacement.dto.ReplacementItemRequest;
import com.taipei.iot.replacement.dto.ReplacementItemResponse;
import com.taipei.iot.replacement.dto.ReplacementOrderQueryParams;
import com.taipei.iot.replacement.dto.ReplacementOrderRequest;
import com.taipei.iot.replacement.dto.ReplacementOrderResponse;
import com.taipei.iot.replacement.dto.SelfCheckRequest;
import com.taipei.iot.replacement.enums.ReplacementOrderStatus;
import com.taipei.iot.replacement.enums.ReplacementOrderType;
import com.taipei.iot.replacement.service.ReplacementItemService;
import com.taipei.iot.replacement.service.ReplacementOrderService;
import com.taipei.iot.user.dto.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

@RestController
@RequestMapping("/v1/auth/replacement/orders")
@RequiredArgsConstructor
public class ReplacementOrderController {

    private final ReplacementOrderService orderService;
    private final ReplacementItemService itemService;

    // ─── 查詢 ───

    @GetMapping
    @PreAuthorize("hasAuthority('REPLACEMENT_VIEW')")
    public BaseResponse<PageResponse<ReplacementOrderResponse>> list(
            @RequestParam(required = false) ReplacementOrderStatus status,
            @RequestParam(required = false) ReplacementOrderType orderType,
            @RequestParam(required = false) Long contractId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ReplacementOrderQueryParams params = ReplacementOrderQueryParams.builder()
                .status(status).orderType(orderType).contractId(contractId).keyword(keyword)
                .dateFrom(dateFrom).dateTo(dateTo).build();
        Page<ReplacementOrderResponse> result = orderService.list(params, PageRequest.of(page, size));
        return BaseResponse.success(toPageResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('REPLACEMENT_VIEW')")
    public BaseResponse<ReplacementOrderResponse> getById(@PathVariable Long id) {
        return BaseResponse.success(orderService.getById(id));
    }

    // ─── 建單 ───

    @PostMapping
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<ReplacementOrderResponse> create(@Valid @RequestBody ReplacementOrderRequest request) {
        return BaseResponse.success(orderService.createDirect(request));
    }

    @PostMapping("/from-repair/{repairTicketId}")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<ReplacementOrderResponse> createFromRepair(
            @PathVariable Long repairTicketId,
            @Valid @RequestBody ReplacementOrderRequest request) {
        return BaseResponse.success(orderService.createFromRepair(repairTicketId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<ReplacementOrderResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody ReplacementOrderRequest request) {
        return BaseResponse.success(orderService.update(id, request));
    }

    // ─── 狀態轉換 ───

    @PostMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> dispatch(@PathVariable Long id,
                                        @Valid @RequestBody ReplacementOrderRequest request) {
        orderService.dispatch(id, request);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/start-work")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> startWork(@PathVariable Long id) {
        orderService.startWork(id);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/self-check")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> selfCheck(@PathVariable Long id,
                                         @Valid @RequestBody SelfCheckRequest request) {
        orderService.selfCheck(id, request);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/submit-review")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> submitReview(@PathVariable Long id) {
        orderService.submitReview(id);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> approve(@PathVariable Long id,
                                       @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.get("comment") : null;
        orderService.approve(id, comment);
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> returnOrder(@PathVariable Long id,
                                           @RequestBody Map<String, String> body) {
        orderService.returnOrder(id, body.get("comment"));
        return BaseResponse.success(null);
    }

    @PostMapping("/{id}/resubmit")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> resubmit(@PathVariable Long id) {
        orderService.resubmit(id);
        return BaseResponse.success(null);
    }

    // ─── 明細 ───

    @GetMapping("/{orderId}/items")
    @PreAuthorize("hasAuthority('REPLACEMENT_VIEW')")
    public BaseResponse<List<ReplacementItemResponse>> getItems(@PathVariable Long orderId) {
        return BaseResponse.success(itemService.getItems(orderId));
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<ReplacementItemResponse> addItem(@PathVariable Long orderId,
                                                          @Valid @RequestBody ReplacementItemRequest request) {
        return BaseResponse.success(itemService.addItem(orderId, request));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<ReplacementItemResponse> updateItem(@PathVariable Long orderId,
                                                             @PathVariable Long itemId,
                                                             @Valid @RequestBody ReplacementItemRequest request) {
        return BaseResponse.success(itemService.updateItem(orderId, itemId, request));
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAuthority('REPLACEMENT_MANAGE')")
    public BaseResponse<Void> deleteItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        itemService.deleteItem(orderId, itemId);
        return BaseResponse.success(null);
    }

    // ─── 工具 ───

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .page(page.getNumber())
                .size(page.getSize())
                .build();
    }
}
