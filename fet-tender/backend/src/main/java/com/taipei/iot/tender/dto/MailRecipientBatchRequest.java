package com.taipei.iot.tender.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class MailRecipientBatchRequest {

    /**
     * Email 清單，每筆至少含 email 欄位。最多一次匯入 100 筆。
     */
    @NotEmpty(message = "匯入清單不得為空")
    @Size(max = 100, message = "一次最多匯入 100 筆")
    private List<String> emails;
}
