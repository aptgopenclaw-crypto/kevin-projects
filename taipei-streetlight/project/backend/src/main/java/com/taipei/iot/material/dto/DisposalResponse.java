package com.taipei.iot.material.dto;

import com.taipei.iot.material.enums.DisposalType;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class DisposalResponse {
    private Long id;
    private Long materialSpecId;
    private String specName;
    private Integer quantity;
    private DisposalType disposalType;
    private String reason;
    private String disposedBy;
    private LocalDateTime disposedAt;
}
