package com.taipei.iot.tender.controller;

import com.taipei.iot.common.response.BaseResponse;
import com.taipei.iot.tender.ai.TenderChatService;
import com.taipei.iot.tender.dto.TenderChatRequest;
import com.taipei.iot.tender.dto.TenderChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 招標 AI 聊天查詢端點。
 *
 * <pre>
 * POST /v1/tender/chat
 * Body: { "message": "查詢近期智慧路燈相關招標", "history": [...] }
 * </pre>
 */
@RestController
@RequestMapping("/v1/tender/chat")
@RequiredArgsConstructor
public class TenderChatController {

    private final TenderChatService chatService;

    @PostMapping
    @PreAuthorize("hasAuthority('tender:chat:use')")
    public BaseResponse<TenderChatResponse> chat(@RequestBody @Valid TenderChatRequest request) {
        return BaseResponse.success(chatService.chat(request));
    }
}
