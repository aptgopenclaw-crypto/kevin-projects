package com.taipei.iot.tender.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TenderDashboardResponse {

    /** KPI 卡片 */
    private KpiCards kpiCards;

    /** 近 30 天每日公告數趨勢 */
    private List<DailyCount> announcementTrend;

    /** 近 6 個月每月決標金額趨勢 */
    private List<MonthlyAmount> awardAmountTrend;

    /** 各方案公告分布（圓餅圖） */
    private List<SolutionDistribution> solutionDistribution;

    /** 最新 5 筆招標公告 */
    private List<RecentAnnouncement> recentAnnouncements;

    @Data
    @Builder
    public static class KpiCards {
        private long announcementCountThisMonth;
        private long announcementCountLastMonth;
        private long awardCountThisMonth;
        private long awardCountLastMonth;
        private BigDecimal awardAmountThisMonth;
        private BigDecimal awardAmountLastMonth;
        private long activeSolutionCount;
    }

    @Data
    @Builder
    public static class DailyCount {
        private String date;   // yyyy-MM-dd
        private long count;
    }

    @Data
    @Builder
    public static class MonthlyAmount {
        private String month;  // yyyy-MM
        private BigDecimal amount;
        private long count;
    }

    @Data
    @Builder
    public static class SolutionDistribution {
        private String solution;
        private long count;
        private BigDecimal totalBudget;
    }

    @Data
    @Builder
    public static class RecentAnnouncement {
        private Long id;
        private String tenderName;
        private String agencyName;
        private String solution;
        private BigDecimal budgetAmount;
        private String announcementDate;
    }
}
