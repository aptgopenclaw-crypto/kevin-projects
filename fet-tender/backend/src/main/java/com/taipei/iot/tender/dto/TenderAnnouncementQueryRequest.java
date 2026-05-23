package com.taipei.iot.tender.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TenderAnnouncementQueryRequest {

    private String solution;
    private String keyword;
    private String agency;
    private String name;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    @Min(0)
    private int page = 0;

    @Min(1) @Max(100)
    private int size = 20;
}
