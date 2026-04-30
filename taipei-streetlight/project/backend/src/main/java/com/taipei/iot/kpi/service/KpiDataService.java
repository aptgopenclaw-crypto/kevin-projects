package com.taipei.iot.kpi.service;

import com.taipei.iot.common.enums.ErrorCode;
import com.taipei.iot.common.exception.BusinessException;
import com.taipei.iot.common.util.SecurityContextUtils;
import com.taipei.iot.kpi.collector.KpiDataCollector;
import com.taipei.iot.kpi.dto.KpiRawDataResponse;
import com.taipei.iot.kpi.entity.KpiIndicator;
import com.taipei.iot.kpi.entity.KpiRawData;
import com.taipei.iot.kpi.enums.KpiIndicatorStatus;
import com.taipei.iot.kpi.enums.KpiRawDataSource;
import com.taipei.iot.kpi.repository.KpiIndicatorRepository;
import com.taipei.iot.kpi.repository.KpiRawDataRepository;
import com.taipei.iot.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiDataService {

    private final KpiRawDataRepository rawDataRepository;
    private final KpiIndicatorRepository indicatorRepository;
    private final List<KpiDataCollector> collectors;

    public Page<KpiRawDataResponse> list(Long indicatorId, Integer periodYear,
                                         Integer periodMonth, Long contractId,
                                         Pageable pageable) {
        return rawDataRepository.findByFilters(indicatorId, periodYear, periodMonth, contractId, pageable)
                .map(this::toResponse);
    }

    /**
     * Excel 匯入 KPI 數據。
     * 格式: indicator_code | period_year | period_month | value | (optional) contract_id
     */
    @Transactional
    public int importFromExcel(MultipartFile file) {
        String tenantId = TenantContext.getCurrentTenantId();
        String username = SecurityContextUtils.getCurrentUsername();
        int count = 0;

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // skip header
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String code = getCellString(row.getCell(0));
                if (code == null || code.isBlank()) continue;

                int year = (int) row.getCell(1).getNumericCellValue();
                int month = (int) row.getCell(2).getNumericCellValue();
                BigDecimal value = BigDecimal.valueOf(row.getCell(3).getNumericCellValue());
                Long contractId = row.getCell(4) != null && row.getCell(4).getCellType() == CellType.NUMERIC
                        ? (long) row.getCell(4).getNumericCellValue() : null;

                count += upsertRawData(tenantId, code, year, month, contractId, value, username);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KPI_DATA_IMPORT_FAILED);
        }
        return count;
    }

    /**
     * CSV 匯入 KPI 數據。
     */
    @Transactional
    public int importFromCsv(MultipartFile file) {
        String tenantId = TenantContext.getCurrentTenantId();
        String username = SecurityContextUtils.getCurrentUsername();
        int count = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) { header = false; continue; }
                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                String code = parts[0].trim();
                int year = Integer.parseInt(parts[1].trim());
                int month = Integer.parseInt(parts[2].trim());
                BigDecimal value = new BigDecimal(parts[3].trim());
                Long contractId = parts.length > 4 && !parts[4].trim().isEmpty()
                        ? Long.parseLong(parts[4].trim()) : null;

                count += upsertRawData(tenantId, code, year, month, contractId, value, username);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.KPI_DATA_IMPORT_FAILED);
        }
        return count;
    }

    /**
     * 自動收集: 呼叫所有 Collector，UPSERT 至 kpi_raw_data。
     */
    @Transactional
    public void autoCollect(LocalDate date, String tenantId) {
        int year = date.getYear();
        int month = date.getMonthValue();

        for (KpiDataCollector collector : collectors) {
            try {
                Map<String, BigDecimal> data = collector.collect(date);
                for (Map.Entry<String, BigDecimal> entry : data.entrySet()) {
                    upsertRawData(tenantId, entry.getKey(), year, month, null,
                            entry.getValue(), "SYSTEM");
                }
                if (!data.isEmpty()) {
                    log.info("{} 收集 {} 筆指標 (tenant={})", collector.getSourceName(), data.size(), tenantId);
                }
            } catch (Exception e) {
                log.error("Collector {} 執行失敗: {}", collector.getSourceName(), e.getMessage(), e);
            }
        }
    }

    // ── helpers ─────────────────────────────────────────────

    private int upsertRawData(String tenantId, String indicatorCode, int year, int month,
                              Long contractId, BigDecimal value, String username) {
        KpiIndicator indicator = indicatorRepository
                .findByTenantIdAndIndicatorCode(tenantId, indicatorCode)
                .orElse(null);
        if (indicator == null || indicator.getStatus() != KpiIndicatorStatus.ACTIVE) {
            log.warn("跳過無效指標: {} (tenant={})", indicatorCode, tenantId);
            return 0;
        }

        // UPSERT logic: find existing or create new
        KpiRawData existing;
        if (contractId != null) {
            existing = rawDataRepository
                    .findByTenantIdAndIndicatorIdAndPeriodYearAndPeriodMonthAndContractId(
                            tenantId, indicator.getId(), year, month, contractId)
                    .orElse(null);
        } else {
            existing = rawDataRepository.findCityLevel(tenantId, indicator.getId(), year, month)
                    .orElse(null);
        }

        if (existing != null) {
            existing.setRawValue(value);
            existing.setImportedAt(LocalDateTime.now());
            existing.setImportedBy(username);
            existing.setSource(username.equals("SYSTEM") ? KpiRawDataSource.AUTO : KpiRawDataSource.MANUAL_IMPORT);
            rawDataRepository.save(existing);
        } else {
            KpiRawData newData = KpiRawData.builder()
                    .indicator(indicator)
                    .periodYear(year)
                    .periodMonth(month)
                    .contractId(contractId)
                    .rawValue(value)
                    .source(username.equals("SYSTEM") ? KpiRawDataSource.AUTO : KpiRawDataSource.MANUAL_IMPORT)
                    .importedBy(username)
                    .build();
            rawDataRepository.save(newData);
        }
        return 1;
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> null;
        };
    }

    private KpiRawDataResponse toResponse(KpiRawData d) {
        return KpiRawDataResponse.builder()
                .id(d.getId())
                .indicatorId(d.getIndicator().getId())
                .indicatorCode(d.getIndicator().getIndicatorCode())
                .indicatorName(d.getIndicator().getIndicatorName())
                .periodYear(d.getPeriodYear())
                .periodMonth(d.getPeriodMonth())
                .contractId(d.getContractId())
                .rawValue(d.getRawValue())
                .source(d.getSource().name())
                .importedAt(d.getImportedAt())
                .importedBy(d.getImportedBy())
                .build();
    }
}
