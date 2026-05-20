package com.taipei.iot.tender.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * 招標 AI 聊天請求 DTO。
 */
@Data
public class TenderChatRequest {

    /** 使用者輸入的訊息（必填） */
    @NotBlank(message = "訊息不得為空")
    private String message;

    /**
     * 對話歷史（選填）。
     * 依序傳入前幾輪的 user / assistant 對話，讓 AI 保有上下文。
     */
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        /** 角色：user 或 assistant */
        private String role;
        /** 訊息內容 */
        private String content;
    }
}
