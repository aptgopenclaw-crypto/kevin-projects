package com.taipei.iot.tender.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MailRecipientBatchResult {

    /** 成功匯入筆數 */
    private int successCount;

    /** 略過（重複或格式錯誤）筆數 */
    private int skippedCount;

    /** 略過的明細 */
    private List<SkippedItem> skippedItems;

    @Data
    @Builder
    public static class SkippedItem {
        private String email;
        private String reason;
    }
}
