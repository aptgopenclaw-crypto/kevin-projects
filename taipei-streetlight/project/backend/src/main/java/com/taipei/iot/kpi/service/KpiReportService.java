package com.taipei.iot.kpi.service;

import com.taipei.iot.device.entity.Contract;
import com.taipei.iot.device.repository.ContractRepository;
import com.taipei.iot.kpi.dto.CompareReportResponse;
import com.taipei.iot.kpi.dto.MonthlyReportResponse;
import com.taipei.iot.kpi.dto.YearlyReportResponse;
import com.taipei.iot.kpi.entity.KpiRawData;
import com.taipei.iot.kpi.entity.KpiResult;
import com.taipei.iot.kpi.repository.KpiRawDataRepository;
import com.taipei.iot.kpi.repository.KpiResultRepository;
import com.taipei.iot.tenant.TenantContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiReportService {

    private final KpiResultRepository resultRepository;
    private final KpiRawDataRepository rawDataRepository;
    private final ContractRepository contractRepository;

    /**
     * 月績效報表。
     */
    public MonthlyReportResponse getMonthlyReport(int year, int month, Long contractId) {
        String tenantId = TenantContext.getCurrentTenantId();

        List<KpiResult> results = contractId != null
                ? resultRepository.findByTenantIdAndPeriodYearAndPeriodMonthAndContractId(tenantId, year, month, contractId)
                : resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month);

        List<MonthlyReportResponse.IndicatorScore> scores = new ArrayList<>();
        BigDecimal totalWeighted = BigDecimal.ZERO;

        for (KpiResult r : results) {
            BigDecimal weight = r.getIndicator().getWeight() != null ? r.getIndicator().getWeight() : BigDecimal.ONE;
            BigDecimal weightedScore = r.getResultValue().multiply(weight);

            // 查找對應的 raw data
            BigDecimal rawValue = rawDataRepository
                    .findByIndicatorIdAndPeriodYearAndPeriodMonth(r.getIndicator().getId(), year, month)
                    .stream()
                    .filter(d -> (contractId == null && d.getContractId() == null)
                            || (contractId != null && contractId.equals(d.getContractId())))
                    .findFirst()
                    .map(KpiRawData::getRawValue)
                    .orElse(null);

            scores.add(MonthlyReportResponse.IndicatorScore.builder()
                    .indicatorCode(r.getIndicator().getIndicatorCode())
                    .indicatorName(r.getIndicator().getIndicatorName())
                    .rawValue(rawValue)
                    .resultValue(r.getResultValue())
                    .targetValue(r.getTargetValue())
                    .achievement(r.getAchievement())
                    .weight(weight)
                    .weightedScore(weightedScore)
                    .build());

            totalWeighted = totalWeighted.add(weightedScore);
        }

        return MonthlyReportResponse.builder()
                .periodYear(year)
                .periodMonth(month)
                .contractId(contractId)
                .totalWeightedScore(totalWeighted.setScale(4, RoundingMode.HALF_UP))
                .indicators(scores)
                .build();
    }

    /**
     * 年度報表 — 12 個月趨勢。
     */
    public YearlyReportResponse getYearlyReport(int year, Long contractId) {
        String tenantId = TenantContext.getCurrentTenantId();

        List<KpiResult> allResults = contractId != null
                ? resultRepository.findByTenantIdAndPeriodYearAndPeriodMonthAndContractId(tenantId, year, null, contractId)
                : resultRepository.findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, null);

        // 月度摘要
        List<YearlyReportResponse.MonthSummary> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            final int month = m;
            BigDecimal total = allResults.stream()
                    .filter(r -> r.getPeriodMonth() == month)
                    .map(r -> r.getResultValue().multiply(
                            r.getIndicator().getWeight() != null ? r.getIndicator().getWeight() : BigDecimal.ONE))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            months.add(YearlyReportResponse.MonthSummary.builder()
                    .month(month)
                    .totalScore(total.setScale(4, RoundingMode.HALF_UP))
                    .build());
        }

        // 指標趨勢
        List<String> indicatorCodes = allResults.stream()
                .map(r -> r.getIndicator().getIndicatorCode())
                .distinct()
                .toList();

        List<YearlyReportResponse.IndicatorTrend> trends = new ArrayList<>();
        for (String code : indicatorCodes) {
            BigDecimal[] monthlyValues = new BigDecimal[12];
            Arrays.fill(monthlyValues, BigDecimal.ZERO);
            String name = "";

            for (KpiResult r : allResults) {
                if (r.getIndicator().getIndicatorCode().equals(code)) {
                    monthlyValues[r.getPeriodMonth() - 1] = r.getResultValue();
                    name = r.getIndicator().getIndicatorName();
                }
            }

            trends.add(YearlyReportResponse.IndicatorTrend.builder()
                    .indicatorCode(code)
                    .indicatorName(name)
                    .monthlyValues(Arrays.asList(monthlyValues))
                    .build());
        }

        return YearlyReportResponse.builder()
                .periodYear(year)
                .contractId(contractId)
                .months(months)
                .indicators(trends)
                .build();
    }

    /**
     * 跨廠商比較報表。
     */
    public CompareReportResponse getCompareReport(int year, int month, List<Long> contractIds) {
        List<CompareReportResponse.ContractScore> contractScores = new ArrayList<>();

        for (Long cid : contractIds) {
            MonthlyReportResponse monthly = getMonthlyReport(year, month, cid);
            String contractName = contractRepository.findById(cid)
                    .map(Contract::getContractName)
                    .orElse("Unknown");

            contractScores.add(CompareReportResponse.ContractScore.builder()
                    .contractId(cid)
                    .contractName(contractName)
                    .totalScore(monthly.getTotalWeightedScore())
                    .indicators(monthly.getIndicators())
                    .build());
        }

        return CompareReportResponse.builder()
                .periodYear(year)
                .periodMonth(month)
                .contracts(contractScores)
                .build();
    }

    /**
     * 匯出報表 (XLS)。
     */
    public void exportXls(MonthlyReportResponse report, OutputStream out) throws IOException {
        try (Workbook wb = new SXSSFWorkbook()) {
            Sheet sheet = wb.createSheet("月績效報表");

            // Header
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("指標代碼");
            header.createCell(1).setCellValue("指標名稱");
            header.createCell(2).setCellValue("原始值");
            header.createCell(3).setCellValue("計算結果");
            header.createCell(4).setCellValue("目標值");
            header.createCell(5).setCellValue("達成率(%)");
            header.createCell(6).setCellValue("權重");
            header.createCell(7).setCellValue("加權分數");

            int rowIdx = 1;
            for (var score : report.getIndicators()) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(score.getIndicatorCode());
                row.createCell(1).setCellValue(score.getIndicatorName());
                if (score.getRawValue() != null) row.createCell(2).setCellValue(score.getRawValue().doubleValue());
                row.createCell(3).setCellValue(score.getResultValue().doubleValue());
                if (score.getTargetValue() != null) row.createCell(4).setCellValue(score.getTargetValue().doubleValue());
                if (score.getAchievement() != null) row.createCell(5).setCellValue(score.getAchievement().doubleValue());
                row.createCell(6).setCellValue(score.getWeight().doubleValue());
                row.createCell(7).setCellValue(score.getWeightedScore().doubleValue());
            }

            // Total row
            Row total = sheet.createRow(rowIdx);
            total.createCell(0).setCellValue("合計");
            total.createCell(7).setCellValue(report.getTotalWeightedScore().doubleValue());

            wb.write(out);
        }
    }

    /**
     * 匯出報表 (CSV)。
     */
    public void exportCsv(MonthlyReportResponse report, OutputStream out) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {
            writer.write("\uFEFF"); // BOM
            writer.write("指標代碼,指標名稱,原始值,計算結果,目標值,達成率(%),權重,加權分數\n");
            for (var score : report.getIndicators()) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s\n",
                        score.getIndicatorCode(),
                        score.getIndicatorName(),
                        score.getRawValue() != null ? score.getRawValue() : "",
                        score.getResultValue(),
                        score.getTargetValue() != null ? score.getTargetValue() : "",
                        score.getAchievement() != null ? score.getAchievement() : "",
                        score.getWeight(),
                        score.getWeightedScore()));
            }
            writer.write(String.format("合計,,,,,,,%s\n", report.getTotalWeightedScore()));
        }
    }
}
