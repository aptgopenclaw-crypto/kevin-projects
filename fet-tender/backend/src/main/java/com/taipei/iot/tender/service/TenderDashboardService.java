package com.taipei.iot.tender.service;

import com.taipei.iot.tender.dto.TenderDashboardResponse;
import com.taipei.iot.tender.dto.TenderDashboardResponse.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TenderDashboardService {

    private final EntityManager em;

    @Transactional(readOnly = true)
    public TenderDashboardResponse getDashboard(String tenantId) {
        return TenderDashboardResponse.builder()
                .kpiCards(buildKpiCards(tenantId))
                .announcementTrend(buildAnnouncementTrend(tenantId))
                .awardAmountTrend(buildAwardAmountTrend(tenantId))
                .solutionDistribution(buildSolutionDistribution(tenantId))
                .recentAnnouncements(buildRecentAnnouncements(tenantId))
                .build();
    }

    private KpiCards buildKpiCards(String tenantId) {
        LocalDate now = LocalDate.now();
        LocalDate thisMonthStart = now.withDayOfMonth(1);
        LocalDate lastMonthStart = thisMonthStart.minusMonths(1);
        LocalDate lastMonthEnd = thisMonthStart.minusDays(1);

        long annThisMonth = countAnnouncements(tenantId, thisMonthStart, now);
        long annLastMonth = countAnnouncements(tenantId, lastMonthStart, lastMonthEnd);
        long awardThisMonth = countAwards(tenantId, thisMonthStart, now);
        long awardLastMonth = countAwards(tenantId, lastMonthStart, lastMonthEnd);
        BigDecimal awardAmtThisMonth = sumAwardAmount(tenantId, thisMonthStart, now);
        BigDecimal awardAmtLastMonth = sumAwardAmount(tenantId, lastMonthStart, lastMonthEnd);
        long activeSolutions = countActiveSolutions(tenantId);

        return KpiCards.builder()
                .announcementCountThisMonth(annThisMonth)
                .announcementCountLastMonth(annLastMonth)
                .awardCountThisMonth(awardThisMonth)
                .awardCountLastMonth(awardLastMonth)
                .awardAmountThisMonth(awardAmtThisMonth)
                .awardAmountLastMonth(awardAmtLastMonth)
                .activeSolutionCount(activeSolutions)
                .build();
    }

    private long countAnnouncements(String tenantId, LocalDate from, LocalDate to) {
        Query q = em.createNativeQuery("""
            SELECT COUNT(*) FROM tender_announcement
            WHERE tenant_id = :tid AND announcement_date BETWEEN :from AND :to
        """);
        q.setParameter("tid", tenantId);
        q.setParameter("from", from);
        q.setParameter("to", to);
        return ((Number) q.getSingleResult()).longValue();
    }

    private long countAwards(String tenantId, LocalDate from, LocalDate to) {
        Query q = em.createNativeQuery("""
            SELECT COUNT(DISTINCT (tender_number, award_announce_date, award_announce_seq))
            FROM tender_award
            WHERE tenant_id = :tid AND award_announce_date BETWEEN :from AND :to
        """);
        q.setParameter("tid", tenantId);
        q.setParameter("from", from);
        q.setParameter("to", to);
        return ((Number) q.getSingleResult()).longValue();
    }

    private BigDecimal sumAwardAmount(String tenantId, LocalDate from, LocalDate to) {
        Query q = em.createNativeQuery("""
            SELECT COALESCE(SUM(award_amount), 0)
            FROM (
                SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq)
                    award_amount
                FROM tender_award
                WHERE tenant_id = :tid AND award_announce_date BETWEEN :from AND :to
                ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
            ) sub
        """);
        q.setParameter("tid", tenantId);
        q.setParameter("from", from);
        q.setParameter("to", to);
        return (BigDecimal) q.getSingleResult();
    }

    private long countActiveSolutions(String tenantId) {
        Query q = em.createNativeQuery("""
            SELECT COUNT(DISTINCT solution) FROM announcement_search_keywords
            WHERE tenant_id = :tid
        """);
        q.setParameter("tid", tenantId);
        return ((Number) q.getSingleResult()).longValue();
    }

    private List<DailyCount> buildAnnouncementTrend(String tenantId) {
        LocalDate from = LocalDate.now().minusDays(29);
        Query q = em.createNativeQuery("""
            SELECT d::date AS day, COALESCE(cnt, 0)
            FROM generate_series(CAST(:from AS date), CURRENT_DATE, '1 day') AS d
            LEFT JOIN (
                SELECT announcement_date, COUNT(*) AS cnt
                FROM tender_announcement
                WHERE tenant_id = :tid AND announcement_date >= CAST(:from AS date)
                GROUP BY announcement_date
            ) t ON t.announcement_date = d::date
            ORDER BY day
        """);
        q.setParameter("tid", tenantId);
        q.setParameter("from", from);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<DailyCount> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Object[] row : rows) {
            java.sql.Date date = (java.sql.Date) row[0];
            long count = ((Number) row[1]).longValue();
            result.add(DailyCount.builder()
                    .date(date.toLocalDate().format(fmt))
                    .count(count)
                    .build());
        }
        return result;
    }

    private List<MonthlyAmount> buildAwardAmountTrend(String tenantId) {
        LocalDate from = LocalDate.now().withDayOfMonth(1).minusMonths(5);
        Query q = em.createNativeQuery("""
            SELECT TO_CHAR(m, 'YYYY-MM') AS month,
                   COALESCE(SUM(sub.award_amount), 0) AS total,
                   COUNT(sub.award_amount) AS cnt
            FROM generate_series(CAST(:from AS date), CURRENT_DATE, '1 month') AS m
            LEFT JOIN (
                SELECT DISTINCT ON (tender_number, award_announce_date, award_announce_seq)
                    award_amount, award_announce_date
                FROM tender_award
                WHERE tenant_id = :tid AND award_announce_date >= CAST(:from AS date)
                ORDER BY tender_number, award_announce_date, award_announce_seq, vendor_order_seq
            ) sub ON TO_CHAR(sub.award_announce_date, 'YYYY-MM') = TO_CHAR(m, 'YYYY-MM')
            GROUP BY month
            ORDER BY month
        """);
        q.setParameter("tid", tenantId);
        q.setParameter("from", from);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<MonthlyAmount> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(MonthlyAmount.builder()
                    .month((String) row[0])
                    .amount((BigDecimal) row[1])
                    .count(((Number) row[2]).longValue())
                    .build());
        }
        return result;
    }

    private List<SolutionDistribution> buildSolutionDistribution(String tenantId) {
        Query q = em.createNativeQuery("""
            SELECT solution, COUNT(*) AS cnt, COALESCE(SUM(budget_amount), 0) AS total
            FROM tender_announcement
            WHERE tenant_id = :tid AND solution IS NOT NULL
            GROUP BY solution
            ORDER BY cnt DESC
            LIMIT 10
        """);
        q.setParameter("tid", tenantId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<SolutionDistribution> result = new ArrayList<>();
        for (Object[] row : rows) {
            result.add(SolutionDistribution.builder()
                    .solution((String) row[0])
                    .count(((Number) row[1]).longValue())
                    .totalBudget((BigDecimal) row[2])
                    .build());
        }
        return result;
    }

    private List<RecentAnnouncement> buildRecentAnnouncements(String tenantId) {
        Query q = em.createNativeQuery("""
            SELECT id, tender_name, agency_name, solution, budget_amount, announcement_date
            FROM tender_announcement
            WHERE tenant_id = :tid
            ORDER BY announcement_date DESC, id DESC
            LIMIT 5
        """);
        q.setParameter("tid", tenantId);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = q.getResultList();
        List<RecentAnnouncement> result = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Object[] row : rows) {
            result.add(RecentAnnouncement.builder()
                    .id(((Number) row[0]).longValue())
                    .tenderName((String) row[1])
                    .agencyName((String) row[2])
                    .solution((String) row[3])
                    .budgetAmount(row[4] != null ? (BigDecimal) row[4] : null)
                    .announcementDate(row[5] != null ? ((java.sql.Date) row[5]).toLocalDate().format(fmt) : null)
                    .build());
        }
        return result;
    }
}
