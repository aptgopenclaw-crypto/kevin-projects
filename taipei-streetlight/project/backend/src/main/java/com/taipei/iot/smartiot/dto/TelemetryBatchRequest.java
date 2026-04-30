package com.taipei.iot.smartiot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TelemetryBatchRequest {

    @NotEmpty(message = "records 不可為空")
    @Size(max = 1000, message = "單次批次上限 1000 筆")
    @Valid
    private List<TelemetryIngestRequest> records;
}
