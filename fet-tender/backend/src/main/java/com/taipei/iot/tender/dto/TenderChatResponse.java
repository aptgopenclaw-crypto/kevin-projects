package com.taipei.iot.tender.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 招標 AI 聊天回應 DTO。
 */
@Data
@Builder
public class TenderChatResponse {

    /** AI 的自然語言回覆 */
    private String message;

    /**
     * 本次對話實際呼叫的最後一個 function 名稱（若有）。
     * 前端可用此欄位判斷 AI 做了哪種查詢。
     */
    private String functionCalled;

    /**
     * 最後一次 function 回傳的原始 JSON 資料（已解析為 Object）。
     * 前端可用此資料直接渲染圖表，不需再解析 AI 文字回覆。
     */
    private Object data;
}
