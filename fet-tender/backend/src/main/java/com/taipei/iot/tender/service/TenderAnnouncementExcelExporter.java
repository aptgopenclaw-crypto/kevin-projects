package com.taipei.iot.tender.service;

import com.taipei.iot.tender.entity.TenderAnnouncement;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 將 TenderAnnouncement 清單匯出為 Excel (XLSX) 位元組陣列。
 */
@Slf4j
@Component
public class TenderAnnouncementExcelExporter {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm");

    private static final String[] HEADERS = {
            "項次", "相關的Solution", "關鍵字", "機關名稱", "標案案號", "標案名稱",
            "採購性質", "招標方式", "公告日期", "截止投標日期",
            "預算金額", "是否訂有底價", "履約地點", "開標時間", "開標地點",
            "詳細連結", "機關代碼", "單位名稱", "機關地址", "聯絡人", "聯絡電話",
            "電子郵件信箱", "標的分類", "採購金額級距"
    };

    public byte[] export(List<TenderAnnouncement> rows) throws IOException {
        SXSSFWorkbook wb = new SXSSFWorkbook(100);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = wb.createSheet("招標資料");

            // ── 標頭列 ─────────────────────────────────────────────────────────
            CellStyle headerStyle = createHeaderStyle(wb);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));
            sheet.createFreezePane(0, 1);

            // ── 欄寬 ───────────────────────────────────────────────────────────
            int[] colWidths = {
                    6*256,  20*256, 12*256, 25*256, 20*256, 40*256,
                    10*256, 12*256, 14*256, 18*256,
                    18*256, 12*256, 25*256, 18*256, 25*256,
                    40*256, 14*256, 20*256, 30*256, 10*256, 16*256,
                    25*256, 30*256, 16*256
            };
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // ── 資料列 ─────────────────────────────────────────────────────────
            CellStyle dateStyle   = createDateStyle(wb);
            CellStyle normalStyle = createNormalStyle(wb);

            for (int i = 0; i < rows.size(); i++) {
                TenderAnnouncement a = rows.get(i);
                Row row = sheet.createRow(i + 1);

                set(row, 0,  i + 1,                                                              normalStyle);
                set(row, 1,  a.getSolution(),                                                    normalStyle);
                set(row, 2,  a.getMatchedKeyword(),                                              normalStyle);
                set(row, 3,  a.getAgencyName(),                                                  normalStyle);
                set(row, 4,  a.getTenderNumber(),                                                normalStyle);
                set(row, 5,  a.getTenderName(),                                                  normalStyle);
                set(row, 6,  a.getProcurementType(),                                             normalStyle);
                set(row, 7,  a.getTenderMethod(),                                                normalStyle);
                set(row, 8,  a.getAnnouncementDate() != null
                        ? a.getAnnouncementDate().format(DATE_FMT) : "",                         dateStyle);
                set(row, 9,  a.getDeadline() != null
                        ? a.getDeadline().format(DATETIME_FMT) : "",                             dateStyle);
                set(row, 10, a.getBudgetAmountRaw(),                                             normalStyle);
                set(row, 11, a.getHasBasePrice() != null
                        ? (a.getHasBasePrice() ? "是" : "否") : "",                             normalStyle);
                set(row, 12, a.getPerformanceLocation(),                                         normalStyle);
                set(row, 13, a.getOpeningTime() != null
                        ? a.getOpeningTime().format(DATETIME_FMT) : "",                          dateStyle);
                set(row, 14, a.getOpeningLocation(),                                             normalStyle);
                set(row, 15, a.getDetailUrl(),                                                   normalStyle);
                set(row, 16, a.getAgencyCode(),                                                  normalStyle);
                set(row, 17, a.getUnitName(),                                                    normalStyle);
                set(row, 18, a.getAgencyAddress(),                                               normalStyle);
                set(row, 19, a.getContactPerson(),                                               normalStyle);
                set(row, 20, a.getContactPhone(),                                                normalStyle);
                set(row, 21, a.getContactEmail(),                                                normalStyle);
                set(row, 22, a.getTenderCategory(),                                              normalStyle);
                set(row, 23, a.getProcurementAmountRange(),                                      normalStyle);
            }

            wb.write(out);
            return out.toByteArray();
        } finally {
            wb.dispose();
        }
    }

    private void set(Row row, int col, Object val, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellStyle(style);
        if (val == null) {
            cell.setCellValue("");
        } else if (val instanceof Integer iv) {
            cell.setCellValue(iv);
        } else {
            cell.setCellValue(val.toString());
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private CellStyle createNormalStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setWrapText(false);
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }
}
