package com.taipei.iot.tender.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taipei.iot.tender.dto.TenderChatRequest;
import com.taipei.iot.tender.dto.TenderChatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 負責與 NVIDIA DeepSeek API 進行對話，並處理 Function Calling 流程：
 *
 * <pre>
 * 1. 組裝 messages（system + history + user）與 tools（function schemas）
 * 2. 呼叫 NVIDIA /chat/completions
 * 3. 若 finish_reason == "tool_calls" → 執行對應查詢 → 將結果加入 messages → 再次呼叫
 * 4. 最終回傳 AI 的自然語言回應
 * </pre>
 */
@Service
@Slf4j
public class TenderChatService {

    private static final int MAX_ROUNDS = 5;

    private static final String SYSTEM_PROMPT = """
            你是一個招標公告查詢助手，專門協助使用者查詢台灣政府採購網的招標資訊。
            當使用者提問時，請使用提供的工具來查詢資料庫，並以繁體中文回答。
            回答時請清楚條列標案名稱、機關名稱、預算金額（元）、公告日期等重要資訊。
            若查無資料，請如實告知並建議使用者調整查詢條件。
            """;

    private final NvidiaAiProperties properties;
    private final RestClient nvidiaRestClient;
    private final TenderFunctionExecutor functionExecutor;
    private final ObjectMapper objectMapper;

    public TenderChatService(
            NvidiaAiProperties properties,
            @Qualifier("nvidiaRestClient") RestClient nvidiaRestClient,
            TenderFunctionExecutor functionExecutor,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.nvidiaRestClient = nvidiaRestClient;
        this.functionExecutor = functionExecutor;
        this.objectMapper = objectMapper;
    }

    public TenderChatResponse chat(TenderChatRequest request) {
        List<Map<String, Object>> messages = buildMessages(request);
        List<Map<String, Object>> tools    = TenderFunctionSchemas.getTools();

        String lastFunctionCalled = null;
        Object lastFunctionData   = null;  // 最後一次 function 的原始資料

        for (int round = 0; round < MAX_ROUNDS; round++) {
            log.debug("AI 呼叫第 {} 輪，message 數量: {}", round + 1, messages.size());

            JsonNode response;
            try {
                response = callApi(messages, tools);
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("NVIDIA API 請求频率上限 (429)，已結束第 {} 輪", round + 1);
                return TenderChatResponse.builder()
                        .message("目前 AI 服務請求過於頒繁，請稍候再試。")
                        .build();
            } catch (HttpClientErrorException e) {
                log.error("NVIDIA API 回傳 HTTP {} 錯誤: {}", e.getStatusCode(), e.getMessage());
                return TenderChatResponse.builder()
                        .message("查詢服務暫時無法使用，請稍後再試。")
                        .build();
            }
            JsonNode choice    = response.path("choices").path(0);
            JsonNode aiMessage = choice.path("message");
            String finishReason = choice.path("finish_reason").asText("stop");

            if ("tool_calls".equals(finishReason) && aiMessage.has("tool_calls")) {
                // --- 將 AI assistant 訊息（含 tool_calls）加入 messages ---
                messages.add(toAssistantMap(aiMessage));

                // --- 執行每一個 tool call ---
                for (JsonNode toolCall : aiMessage.get("tool_calls")) {
                    String toolCallId = toolCall.path("id").asText();
                    String funcName   = toolCall.path("function").path("name").asText();
                    String funcArgs   = toolCall.path("function").path("arguments").asText("{}");

                    log.info("執行 function: {}, args: {}", funcName, funcArgs);
                    lastFunctionCalled = funcName;

                    String result = functionExecutor.execute(funcName, funcArgs);
                    log.debug("function {} 回傳: {}", funcName, result);

                    // 嘗試解析 JSON 供前端圖表使用
                    try {
                        lastFunctionData = objectMapper.readValue(result, Object.class);
                    } catch (Exception e) {
                        lastFunctionData = null;
                    }

                    // 將 tool 結果加入 messages
                    messages.add(Map.of(
                            "role",         "tool",
                            "tool_call_id", toolCallId,
                            "content",      result
                    ));
                }
                // 繼續下一輪
            } else {
                // --- 最終回應 ---
                String content = aiMessage.has("content") && !aiMessage.get("content").isNull()
                        ? aiMessage.get("content").asText()
                        : "抱歉，目前無法取得回應，請稍後再試。";

                return TenderChatResponse.builder()
                        .message(content)
                        .functionCalled(lastFunctionCalled)
                        .data(lastFunctionData)
                        .build();
            }
        }

        return TenderChatResponse.builder()
                .message("已達查詢次數上限，請重新提問或縮小查詢範圍。")
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 私有輔助方法
    // ─────────────────────────────────────────────────────────────────────────

    private List<Map<String, Object>> buildMessages(TenderChatRequest request) {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

        if (request.getHistory() != null) {
            for (var h : request.getHistory()) {
                messages.add(Map.of("role", h.getRole(), "content", h.getContent()));
            }
        }

        messages.add(Map.of("role", "user", "content", request.getMessage()));
        return messages;
    }

    private JsonNode callApi(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model",       properties.getModel());
        body.put("messages",    messages);
        body.put("tools",       tools);
        body.put("tool_choice", "auto");
        body.put("temperature", properties.getTemperature());
        body.put("max_tokens",  properties.getMaxTokens());

        String endpoint = properties.getBaseUrl();
        if (endpoint.endsWith("/")) endpoint = endpoint.substring(0, endpoint.length() - 1);
        endpoint += "/chat/completions";

        log.info("呼叫 NVIDIA API: {}", endpoint);

        return nvidiaRestClient.post()
                .uri(endpoint)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
    }

    /**
     * 將 AI 回傳的 assistant message JsonNode 轉換為可再送出的 Map，
     * 保留 tool_calls 結構（Jackson 會正確序列化 JsonNode）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> toAssistantMap(JsonNode aiMessage) {
        Map<String, Object> map = new HashMap<>();
        map.put("role", "assistant");

        if (aiMessage.has("content") && !aiMessage.get("content").isNull()) {
            map.put("content", aiMessage.get("content").asText());
        } else {
            map.put("content", null);
        }

        if (aiMessage.has("tool_calls")) {
            try {
                // 轉成 List<Map> 以便 Jackson 能正確序列化送回
                map.put("tool_calls", objectMapper.convertValue(aiMessage.get("tool_calls"), List.class));
            } catch (Exception e) {
                log.warn("tool_calls 轉換失敗", e);
            }
        }
        return map;
    }
}
