package com.taipei.iot.dashboard.service;

import com.taipei.iot.dashboard.dto.KpiSummaryResponse;
import com.taipei.iot.dashboard.dto.KpiTrendResponse;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.entity.KpiResult;
import com.taipei.iot.kpi.repository.KpiResultRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WidgetKpiService {

    private final KpiResultRepository resultRepository;

    public KpiSummaryResponse getSummary(int year, int month) {
        String tenantId = TenantContext.getCurrentTenantId();

        List<KpiResult> results = resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month);

        List<KpiSummaryResponse.KpiIndicatorSummary> items = new ArrayList<>();
        for (KpiResult r : results) {
            KpiIndicator ind = r.getIndicator();
            if (ind == null) continue;

            items.add(KpiSummaryResponse.KpiIndicatorSummary.builder()
                    .code(ind.getIndicatorCode())
                    .name(ind.getIndicatorName())
                    .value(r.getResultValue())
                    .target(r.getTargetValue())
                    .achievement(r.getAchievement())
                    .grade(calcGrade(r.getAchievement()))
                    .build());
        }

        return KpiSummaryResponse.builder().indicators(items).build();
    }

    public KpiTrendResponse getTrend(int year, int month, int months) {
        String tenantId = TenantContext.getCurrentTenantId();

        List<KpiTrendResponse.MonthlyKpi> monthlyList = new ArrayList<>();

        for (int i = months - 1; i >= 0; i--) {
            int m = month - i;
            int y = year;
            while (m <= 0) { m += 12; y--; }

            List<KpiResult> results = resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, y, m);

            List<KpiTrendResponse.IndicatorValue> values = new ArrayList<>();
            for (KpiResult r : results) {
                KpiIndicator ind = r.getIndicator();
                if (ind == null) continue;
                values.add(KpiTrendResponse.IndicatorValue.builder()
                        .code(ind.getIndicatorCode())
                        .value(r.getResultValue())
                        .build());
            }

            monthlyList.add(KpiTrendResponse.MonthlyKpi.builder()
                    .month(String.format("%d-%02d", y, m))
                    .indicators(values)
                    .build());
        }

        return KpiTrendResponse.builder().months(monthlyList).build();
    }

    private String calcGrade(BigDecimal achievement) {
        if (achievement == null) return "F";
        double a = achievement.doubleValue();
        if (a >= 90) return "A";
        if (a >= 80) return "B";
        if (a >= 70) return "C";
        if (a >= 60) return "D";
        return "F";
    }
}
