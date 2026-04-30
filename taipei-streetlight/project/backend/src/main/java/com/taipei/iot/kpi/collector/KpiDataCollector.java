package com.taipei.iot.kpi.collector;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * KPI 資料收集器介面。
 * 各模組實作此介面以提供自動化數據收集。
 */
public interface KpiDataCollector {

    /**
     * 收集指定日期的 KPI 原始數據。
     *
     * @param date 目標日期
     * @return Map: key=指標代碼, value=原始數值。若無數據回傳空 Map。
     */
    Map<String, BigDecimal> collect(LocalDate date);

    /**
     * 此收集器負責的資料來源名稱。
     */
    String getSourceName();
}
