package com.taipei.iot.tender.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Spring Data JPA native query projection interfaces for vendor dashboard queries.
 * Column aliases in SQL (snake_case) are auto-mapped to getter names (camelCase).
 */
public final class VendorProjections {

    private VendorProjections() {}

    /** 廠商搜尋建議 */
    public interface Suggest {
        String getVendorName();
        String getVendorTaxId();
        Long getWinCount();
    }

    /** 廠商資料日期範圍（用於自動判斷趨勢粒度） */
    public interface DateRange {
        LocalDate getMinDate();
        LocalDate getMaxDate();
    }

    /** KPI 摘要 */
    public interface Overview {
        Long getTotalWins();
        BigDecimal getTotalAmount();
        Long getAgencyCount();
        Long getSolutionCount();
        LocalDate getFirstAwardDate();
        LocalDate getLatestAwardDate();
        String getVendorName();
        String getVendorTaxId();
    }

    /** 趨勢折線一個時間點 */
    public interface Trend {
        String getPeriod();
        Long getCount();
        BigDecimal getTotalAmount();
    }

    /** Solution × Keyword 分布一筆 */
    public interface SolutionRow {
        String getSolution();
        String getKeyword();
        Long getCount();
        BigDecimal getTotalAmount();
    }

    /** 機關排行一筆 */
    public interface Agency {
        String getAgencyName();
        String getAgencyCode();
        Long getCount();
        BigDecimal getTotalAmount();
    }

    /** 採購屬性分布（招標方式 / 採購類型 / 決標方式） */
    public interface TypeCount {
        String getTypeName();
        Long getCount();
    }

    /** 共同得標廠商一筆 */
    public interface CoVendor {
        String getVendorName();
        String getVendorTaxId();
        Long getCoCount();
    }

    /** Solution 競品分析：廠商排行一筆 */
    public interface SolutionVendorRank {
        String getVendorName();
        String getVendorTaxId();
        Long getWinCount();
        BigDecimal getTotalAmount();
    }

    /** Solution 競品分析：關鍵字摘要一筆 */
    public interface SolutionKeyword {
        String getKeyword();
        Long getVendorCount();
        Long getWinCount();
        BigDecimal getTotalAmount();
    }

    /** Solution 競品分析：總覽摘要 */
    public interface SolutionOverview {
        Long getTotalTenders();
        BigDecimal getTotalAmount();
        Long getVendorCount();
        Long getKeywordCount();
    }

    /** Solution 下拉選單選項 */
    public interface SolutionOption {
        String getSolution();
    }
}
