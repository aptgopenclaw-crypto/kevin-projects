package com.taipei.iot.material.dto;

import lombok.*;

import java.util.List;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ImportResult {
    private int successCount;
    private int skippedCount;
    private List<String> errors;
}
