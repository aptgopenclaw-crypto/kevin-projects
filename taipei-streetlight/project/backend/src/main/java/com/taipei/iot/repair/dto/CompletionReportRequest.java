package com.taipei.iot.repair.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class CompletionReportRequest {

    @NotBlank(message = "維修描述為必填")
    private String repairDescription;
    private String faultCause;
    private List<AttachmentUploadRequest> attachments;
}
