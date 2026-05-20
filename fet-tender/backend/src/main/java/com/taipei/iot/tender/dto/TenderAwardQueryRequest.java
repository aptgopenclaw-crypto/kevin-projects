package com.taipei.iot.tender.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class TenderAwardQueryRequest {

    private String solution;
    private String keyword;
    private String agency;
    private String name;
    private String vendorName;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    private int page = 0;
    private int size = 20;
}
