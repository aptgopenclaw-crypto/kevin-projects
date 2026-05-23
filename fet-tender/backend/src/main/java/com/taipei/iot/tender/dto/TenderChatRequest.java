package com.taipei.iot.tender.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 招標 AI 聊天請求 DTO。
 */
@Data
public class TenderChatRequest {

    /** 使用者輸入的訊息（必填，最多 500 字） */
    @NotBlank(message = "訊息不得為空")
    @Size(max = 500, message = "訊息長度不得超過 500 字")
    private String message;

    /**
     * 對話歷史（選填，最多 20 則）。
     * 依序傳入前幾輪的 user / assistant 對話，讓 AI 保有上下文。
     */
    @Size(max = 20, message = "對話歷史最多 20 則")
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        /** 角色：user 或 assistant */
        private String role;
        /** 訊息內容 */
        private String content;
    }
}
